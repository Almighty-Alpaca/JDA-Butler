package com.kantenkugel.discordbot.versioncheck;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class VersionChecker
{
    public static final Logger LOG = LoggerFactory.getLogger(VersionChecker.class);

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, MiscUtils.newThreadFactory("versionchecker-thread", LOG));

    public static Set<Pair<VersionedItem, String>> checkVersions()
    {
        Set<Pair<VersionedItem, String>> changedItems = new HashSet<>();
        VersionCheckerRegistry.getVersionedItems().forEach(item -> {
            String version = getVersion(item);
            if (version != null && (item.getVersion() == null || !item.getVersion().equals(version)))
            {
                changedItems.add(Pair.of(item, item.getVersion()));
                item.setVersion(version);
            }
        });
        return changedItems;
    }

    static String getVersion(VersionedItem item)
    {
        try
        {
            Supplier<String> versionSupplier = item.getCustomVersionSupplier();
            if(versionSupplier != null)
                return versionSupplier.get();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            try(Response res = Bot.httpClient.newCall(new Request.Builder()
                    .url(item.getRepoUrl()).get().build()).execute())
            {

                if (!res.isSuccessful())
                {
                    LOG.warn("Could not fetch Maven metadata from " + item.getRepoUrl() + " - OkHttp returned with failure");
                    return null;
                }

                Document doc = dBuilder.parse(res.body().byteStream());

                Element root = doc.getDocumentElement();
                root.normalize();

                Element versioningElem = (Element) root.getElementsByTagName("versioning").item(0);
                if (versioningElem == null)
                {
                    LOG.warn("Could not find versioning node for item {}", item.getName());
                    return null;
                }

                Element versionElem = (Element) versioningElem.getElementsByTagName("release").item(0);
                if (versionElem == null)
                {
                    LOG.warn("Could not find release node for item {}", item.getName());
                    return null;
                }

                return versionElem.getTextContent();
            }
        }
        catch(SocketTimeoutException | UncheckedIOException ex)
        {
            if(ex instanceof SocketTimeoutException || ex.getCause().getClass() == SocketTimeoutException.class)
                LOG.warn("Version-fetch for item {} timed out", item.getName());
            else
                LOG.error("Could not fetch version info for item {}", item.getName(), ex);
        }
        catch (Exception e)
        {
            LOG.error("Could not fetch version info for item {}", item.getName(), e);
        }
        return null;
    }

    static void initUpdateLoop()
    {
        final int delayMinutes = 1 + (VersionCheckerRegistry.getVersionedItems().size() / 5);
        EXECUTOR.scheduleWithFixedDelay(() ->
        {
            LOG.debug("Checking for updates...");

            Set<Pair<VersionedItem, String>> changedItems;
            Future<Set<Pair<VersionedItem, String>>> check = EXECUTOR.submit(VersionChecker::checkVersions);
            try
            {
                changedItems = check.get(delayMinutes, TimeUnit.MINUTES);
            }
            catch(TimeoutException ex)
            {
                check.cancel(true);
                LOG.warn("Version-checking timed out");
                return;
            }
            catch(Exception ex)
            {
                LOG.error("There was an error fetching newest versions", ex);
                return;
            }

            boolean shouldAnnounce = !Bot.config.getBoolean("testing", true) && !Bot.isStealth;

            for (Pair<VersionedItem, String> changedItemPair : changedItems)
            {
                VersionedItem changedItem = changedItemPair.getLeft();
                if(changedItem.getUpdateHandler() == null)
                    continue;
                Future<?> updateTask = EXECUTOR.submit(() ->
                {
                    try
                    {
                        changedItem.getUpdateHandler().onUpdate(changedItem, changedItemPair.getRight(), shouldAnnounce);
                    }
                    catch(Exception ex)
                    {
                        LOG.warn("UpdateHandler for {} failed", changedItem.getName(), ex);
                    }
                });
                try
                {
                    updateTask.get(30, TimeUnit.SECONDS);
                }
                catch(TimeoutException e)
                {
                    updateTask.cancel(true);
                    LOG.warn("UpdateHandler for {} timed out!", changedItem.getName());
                }
                catch(Exception e)
                {
                    LOG.error("There was an error executing the UpdateHandler for {}", changedItem.getName());
                }
            }
        }, delayMinutes, delayMinutes, TimeUnit.MINUTES);
    }
}

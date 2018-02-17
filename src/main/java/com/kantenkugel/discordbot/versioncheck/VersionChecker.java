package com.kantenkugel.discordbot.versioncheck;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VersionChecker
{
    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(run ->
    {
        Thread t = new Thread(run);
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) -> throwable.printStackTrace());
        return t;
    });
    static final Logger LOG = LoggerFactory.getLogger(VersionChecker.class);

    public static Set<VersionedItem> checkVersions()
    {
        Set<VersionedItem> changedItems = new HashSet<>();
        VersionCheckerRegistry.getVersionedItems().forEach(item -> {
            String version = getVersion(item);
            if (version != null && (item.getVersion() == null || !item.getVersion().equals(version)))
            {
                item.setVersion(version);
                changedItems.add(item);
            }
        });
        return changedItems;
    }

    static String getVersion(VersionedItem item)
    {
        ResponseBody body = null;
        try
        {
            if(item.getCustomVersionSupplier() != null)
                return item.getCustomVersionSupplier().get();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Response res = Bot.httpClient.newCall(
                    new Request.Builder().url(item.getRepoUrl()).get().build()
            ).execute();

            if (!res.isSuccessful())
            {
                LOG.warn("Could not fetch Maven metadata from " + item.getRepoUrl() + " - OkHttp returned with failure");
                return null;
            }

            body = res.body();
            Document doc = dBuilder.parse(body.byteStream());

            Element root = doc.getDocumentElement();
            root.normalize();

            Element versioningElem = (Element) root.getElementsByTagName("versioning").item(0);
            if (versioningElem == null)
            {
                LOG.warn("Could not find versioning node");
                return null;
            }

            Element versionElem = (Element) versioningElem.getElementsByTagName("release").item(0);
            if (versionElem == null)
            {
                LOG.warn("Could not find release node");
                return null;
            }

            return versionElem.getTextContent();

        } catch (Exception e)
        {
            LOG.warn("Could not fetch Maven metadata from " + item.getRepoUrl());
            //e.printStackTrace();
        }
        finally
        {
            if(body != null)
                body.close();
        }
        return null;
    }

    static void initUpdateLoop()
    {
        EXECUTOR.scheduleAtFixedRate(() ->
        {
            Bot.LOG.debug("Checking for updates...");

            Set<VersionedItem> changedItems = VersionChecker.checkVersions();

            if (Bot.config.getBoolean("testing", true))
            {
                Bot.LOG.debug("Skipping version update handlers!");
                return;
            }

            for (VersionedItem changedItem : changedItems)
            {
                if(changedItem.getUpdateHandler() == null)
                    continue;
                try
                {
                    changedItem.getUpdateHandler().accept(changedItem);
                }
                catch(Exception ex)
                {
                    Bot.LOG.warn("UpdateHandler for {} failed", changedItem.getName());
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
}

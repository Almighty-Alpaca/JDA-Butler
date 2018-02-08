package com.kantenkugel.discordbot.versioncheck;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
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

public class VersionChecker
{
    static final Logger LOG = LoggerFactory.getLogger(VersionChecker.class);

    private static final Map<String, VersionedItem> checkedItems = new LinkedHashMap<>();

    static {
        addItem(new VersionedItem("JDA", RepoType.JCENTER, "net.dv8tion", "JDA",
                JenkinsApi.LAST_BUILD_URL));
        addItem(new VersionedItem("Lavaplayer", RepoType.JCENTER, "com.sedmelluq", "lavaplayer",
                "https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord"));
        addItem(new VersionedItem("JDA-Utilities", RepoType.JCENTER, "com.jagrosh", "jda-utilities",
                "https://github.com/JDA-Applications/JDA-Utilities"));
    }

    public static Set<VersionedItem> checkVersions()
    {
        Set<VersionedItem> changedItems = new HashSet<>();
        checkedItems.values().forEach(item -> {
            String version = getVersion(item);
            if (version != null && (item.getVersion() == null || !item.getVersion().equals(version)))
            {
                item.setVersion(version);
                changedItems.add(item);
            }
        });
        return changedItems;
    }

    public static void addItem(String name, String repoType, String groupId, String artifactId, String url)
    {
        addItem(new VersionedItem(name, RepoType.fromString(repoType), groupId, artifactId, url));
    }

    public static boolean addItem(VersionedItem item)
    {
        String version = getVersion(item);
        if (version != null)
        {
            item.setVersion(version);
            checkedItems.put(item.getName().toLowerCase(), item);
            return true;
        }
        return false;
    }

    public static void removeItem(VersionedItem item)
    {
        removeItem(item.getName());
    }

    public static void removeItem(String name)
    {
        checkedItems.remove(name.toLowerCase());
    }

    public static VersionedItem getItem(String name)
    {
        return checkedItems.get(name.toLowerCase());
    }

    public static Collection<VersionedItem> getVersionedItems()
    {
        return checkedItems.values();
    }

    private static String getVersion(VersionedItem item)
    {
        ResponseBody body = null;
        try
        {
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
}

package com.kantenkugel.discordbot.versioncheck;

import net.dv8tion.jda.core.utils.SimpleLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public class VersionChecker
{
    private static final SimpleLog LOG = SimpleLog.getLog("VersionChecker");

    private static Map<String, VersionedItem> checkedItems = new LinkedHashMap<>();

    static {
        addItem(new VersionedItem("JDA", RepoType.JCENTER, "net.dv8tion", "JDA",
                "http://home.dv8tion.net:8080/job/JDA/lastSuccessfulBuild/"));
        addItem(new VersionedItem("Lavaplayer", RepoType.JCENTER, "com.sedmelluq", "lavaplayer",
                "https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord"));
        addItem(new VersionedItem("JDA-Utilities", RepoType.JCENTER, "com.jagrosh", "JDA-Utilities",
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

    public static void addItem(VersionedItem item)
    {
        String version = getVersion(item);
        if (version != null)
        {
            item.setVersion(version);
            checkedItems.put(item.getName().toLowerCase(), item);
        }
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
        try
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(item.getRepoUrl());

            Element root = doc.getDocumentElement();
            root.normalize();

            Element versioningElem = (Element) root.getElementsByTagName("versioning").item(0);
            if(versioningElem == null)
            {
                LOG.warn("Could not find versioning node");
                return null;
            }

            Element versionElem = (Element) versioningElem.getElementsByTagName("release").item(0);
            if(versionElem == null)
            {
                LOG.warn("Could not find release node");
                return null;
            }

            return versionElem.getTextContent();

        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}

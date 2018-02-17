package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.versioncheck.items.*;

import java.util.*;

public class VersionCheckerRegistry
{
    private static final Map<String, VersionedItem> checkedItems = new LinkedHashMap<>();

    public static void addItem(String name, String repoType, String groupId, String artifactId, String url)
    {
        addItem(new SimpleVersionedItem(name, RepoType.fromString(repoType), DependencyType.DEFAULT,
                groupId, artifactId, url, null));
    }

    public static boolean addItem(VersionedItem item)
    {
        String version = VersionChecker.getVersion(item);
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
        String lowerName = name.toLowerCase();
        VersionedItem item = checkedItems.get(lowerName);
        if(item == null)
        {
            item = getVersionedItems().stream()
                    .filter(i -> i.getAliases() != null && i.getAliases().contains(lowerName))
                    .findAny().orElse(null);
        }
        return item;
    }

    public static Collection<VersionedItem> getVersionedItems()
    {
        return checkedItems.values();
    }

    private static boolean initialized = false;
    public synchronized static void init()
    {
        if(initialized)
            return;
        initialized = true;
        register();
        VersionChecker.initUpdateLoop();
    }

    private static void register()
    {
        //Core
        addItem(new JDAItem());
        addItem(new SimpleVersionedItem("Lavaplayer", RepoType.JCENTER, DependencyType.DEFAULT,
                "com.sedmelluq", "lavaplayer",
                "https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord",
                Arrays.asList("lava", "player")));
        addItem(new SimpleVersionedItem("JDA-Utilities", RepoType.JCENTER, DependencyType.POM,
                "com.jagrosh", "jda-utilities",
                "https://github.com/JDA-Applications/JDA-Utilities",
                Arrays.asList("utils", "jda-utils")));
        //featured
        addItem(new YuiItem());
        addItem(new JDActionItem());
        //other
    }
}

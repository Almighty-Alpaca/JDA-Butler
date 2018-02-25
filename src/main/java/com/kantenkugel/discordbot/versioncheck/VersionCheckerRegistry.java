package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.versioncheck.items.*;

import java.util.*;
import java.util.stream.Collectors;

public class VersionCheckerRegistry
{
    private static final Map<String, VersionedItem> checkedItems = new LinkedHashMap<>();

    public static void addItem(String name, String repoType, String groupId, String artifactId, String url)
    {
        addItem(new SimpleVersionedItem(name, RepoType.fromString(repoType), DependencyType.DEFAULT, groupId, artifactId)
                .setUrl(url)
        );
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

    /**
     * Accepts a space delimited string of item names or aliases and returns a List of all found VersionedItems.
     * Duplicates are already removed.
     *
     * @param spaceDelimString
     *          A String with space delimited VersionedItem names or aliases
     * @param prependJDA
     *          If set to true, there will be a single JDA VersionedItem prepended to the returned list
     * @return  All found VersionedItems
     */
    public static List<VersionedItem> getItemsFromString(String spaceDelimString, boolean prependJDA)
    {
        //since distinct() preserves first element by definition,
        //this is best way of enforcing jda to be at first position
        if(prependJDA)
            spaceDelimString = "jda " + spaceDelimString;

        String[] split = spaceDelimString.trim().toLowerCase().split("\\s+");
        return Arrays.stream(split)
                .map(VersionCheckerRegistry::getItem)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
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
        addItem(new SimpleVersionedItem("Lavaplayer", RepoType.JCENTER, DependencyType.DEFAULT, "com.sedmelluq", "lavaplayer")
                .setUrl("https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord")
                .setAliases("lava", "player")
                .setAnnouncementRoleId(241948768113524762L)
                .setAnnouncementChannelId(263484072389640193L)
        );
        addItem(new SimpleVersionedItem("JDA-Utilities", RepoType.JCENTER, DependencyType.POM, "com.jagrosh", "jda-utilities")
                .setUrl("https://github.com/JDA-Applications/JDA-Utilities")
                .setAliases("utils", "jda-utils")
                .setAnnouncementRoleId(417331483091664896L)
                .setAnnouncementChannelId(384483855475933184L)
        );
        //featured
        addItem(new YuiItem());
        addItem(new JDActionItem());
        //other
    }
}

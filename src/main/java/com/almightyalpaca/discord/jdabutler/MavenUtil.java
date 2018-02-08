package com.almightyalpaca.discord.jdabutler;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;

public class MavenUtil
{
    public static String getDependencyString(final VersionedItem item, String indentation)
    {
        if (indentation == null)
            indentation = "";
        String typeString = item.getDependencyType() == DependencyType.DEFAULT
                ? ""
                : indentation + "    <type>" + item.getDependencyType().getTypeString() + "</type>\n";
        return indentation + "<dependency>\n"
                + indentation + "    <groupId>" + item.getGroupId() + "</groupId>\n"
                + indentation + "    <artifactId>" + item.getArtifactId() + "</artifactId>\n"
                + indentation + "    <version>" + item.getVersion() + "</version>\n"
                + typeString
                + indentation + "</dependency>\n";
    }

    public static String getRepositoryString(final String id, final String name, final String url, String indentation)
    {
        if (indentation == null)
            indentation = "";
        return indentation + "<repository>\n" + indentation + "    <id>" + id + "</id>\n" + indentation + "    <name>" + name + "</name>\n" + indentation + "    <url>" + url + "</url>\n" + indentation + "</repository>\n";
    }

}

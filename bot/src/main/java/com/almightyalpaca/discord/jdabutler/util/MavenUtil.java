package com.almightyalpaca.discord.jdabutler.util;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;

import java.util.List;
import java.util.Objects;

public class MavenUtil
{
    public static String getDependencyBlock(final List<VersionedItem> items, String indent)
    {
        if(indent == null)
            indent = "";
        StringBuilder text = new StringBuilder(indent).append("<dependencies>\n");
        String subIndent = indent + "    ";
        for (final VersionedItem item : items)
            text.append(getDependencyString(item, subIndent));
        text.append(indent).append("</dependencies>");
        return text.toString();
    }

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

    public static String getRepositoryBlock(final List<VersionedItem> items, String indent)
    {
        if(indent == null)
            indent = "";
        String subIndent = indent + "    ";
        StringBuilder text = new StringBuilder(indent).append("<repositories>\n");
        items.stream()
                .map(VersionedItem::getRepoType)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(type ->
                        text.append(getRepositoryString(type.toString(), type.toString(), type.getRepoBase(), subIndent))
                );
        text.append(indent).append("</repositories>");
        return text.toString();
    }

    public static String getRepositoryString(final String id, final String name, final String url, String indentation)
    {
        if (indentation == null)
            indentation = "";
        return indentation + "<repository>\n" + indentation + "    <id>" + id + "</id>\n" + indentation + "    <name>" + name + "</name>\n" + indentation + "    <url>" + url + "</url>\n" + indentation + "</repository>\n";
    }

}

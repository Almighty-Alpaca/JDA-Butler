package com.almightyalpaca.discord.jdabutler.util;

import com.kantenkugel.discordbot.versioncheck.DependencyType;
import com.kantenkugel.discordbot.versioncheck.RepoType;
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
                .filter(item -> item.getRepoType() != null)
                .flatMap(item -> item.getAllRepositories().stream())
                .filter(Objects::nonNull)
                .distinct()
                .filter(item -> item != RepoType.MAVENCENTRAL)
                .sorted()
                .forEach(type ->
                        text.append(getRepositoryString(type, subIndent))
                );
        text.append(indent).append("</repositories>");
        return text.toString();
    }

    public static String getRepositoryString(final RepoType repoType, String indentation)
    {
        if (indentation == null)
            indentation = "";
        String name = repoType.getName();
        return indentation + "<repository>\n" + indentation + "    <id>" + name + "</id>\n" + indentation + "    <name>" + name + "</name>\n" + indentation + "    <url>" + repoType.getRepoBase() + "</url>\n" + indentation + "</repository>\n";
    }

}

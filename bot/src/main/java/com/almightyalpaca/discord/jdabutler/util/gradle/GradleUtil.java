package com.almightyalpaca.discord.jdabutler.util.gradle;

import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class GradleUtil
{

    public static final Collection<Pair<String, String>> DEFAULT_PLUGINS = Arrays.asList(new ImmutablePair<>("java", null), new ImmutablePair<>("application", null), new ImmutablePair<>("com.github.johnrengelman.shadow", GradleUtil.SHADOW_VERSION));

    public static final String SHADOW_VERSION = "4.0.4";

    public static String getBuildFile(final Collection<Pair<String, String>> plugins, final String mainClassName, final String version, final String sourceCompatibility, final List<VersionedItem> items, final boolean pretty)
    {
        return GradleUtil.getPluginsBlock(plugins) +
                "\n\nmainClassName = '" + mainClassName + '\'' +
                "\n\nversion '" + version + '\'' +
                "\n\nsourceCompatibility = " + sourceCompatibility +
                "\n\n" +
                GradleUtil.getRepositoryBlock(items) +
                "\n\n" +
                GradleUtil.getDependencyBlock(items, pretty) +
                "\n\ncompileJava.options.encoding = 'UTF-8'\n";
    }

    public static String getDependencyBlock(final List<VersionedItem> items, final boolean pretty)
    {
        StringBuilder text = new StringBuilder("dependencies {\n");
        for (final VersionedItem item : items)
            text.append("    ").append(GradleUtil.getDependencyString(item, pretty)).append("\n");
        text.append("}");
        return text.toString();
    }

    public static String getDependencyString(final VersionedItem item, final boolean pretty)
    {
        if (pretty)
            return String.format("compile group: '%s', name: '%s', version: '%s'",
                    item.getGroupId(), item.getArtifactId(), item.getVersion());
        else
            return String.format("compile '%s:%s:%s'", item.getGroupId(), item.getArtifactId(), item.getVersion());
    }

    public static String getPluginsBlock(final Collection<Pair<String, String>> plugins)
    {
        StringBuilder text = new StringBuilder("plugins {\n");
        for (final Pair<String, String> plugin : plugins)
        {
            text.append("    id'").append(plugin.getLeft()).append("'");
            if (plugin.getRight() != null)
                text.append(" version '").append(plugin.getRight()).append("'");
            text.append("\n");
        }
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryBlock(final List<VersionedItem> items)
    {
        StringBuilder text = new StringBuilder("repositories {\n");
        items.stream()
                .map(item -> item.getRepoType().getGradleImport())
                .filter(Objects::nonNull)
                .distinct()
                .forEach(pair ->
                        text.append(GradleUtil.getRepositoryString(pair.getLeft(), pair.getRight(), "    ")).append("\n")
                );
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryString(final String name, final String url, String indentation)
    {
        if (indentation == null)
            indentation = "";
        if (url == null)
            return indentation + name;
        else
            return indentation + "maven {\n" + indentation + "    name '" + name + "'\n" + indentation + "    url '" + url + "'\n" + indentation + "}";
    }
}

package com.almightyalpaca.discord.jdabutler.util.gradle;

import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class GradleUtil
{

    public static final Collection<Pair<String, String>> DEFAULT_PLUGINS = Arrays.asList(new ImmutablePair<>("java", null), new ImmutablePair<>("application", null), new ImmutablePair<>("com.github.johnrengelman.shadow", GradleUtil.SHADOW_VERSION));

    public static final String SHADOW_VERSION = "6.0.0";

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
            return String.format("implementation group: '%s', name: '%s', version: '%s'",
                    item.getGroupId(), item.getArtifactId(), item.getVersion());
        else
            return String.format("implementation '%s:%s:%s'", item.getGroupId(), item.getArtifactId(), item.getVersion());
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
                .filter(item -> item.getRepoType() != null)
                .flatMap(item -> item.getAllRepositories().stream())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(repoType ->
                        text.append(GradleUtil.getRepositoryString(repoType, "    ")).append("\n")
                );
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryString(final RepoType repoType, String indentation)
    {
        if (indentation == null)
            indentation = "";
        if (repoType.getGradleName() != null)
            return indentation + repoType.getGradleName() + "()";
        else
            return indentation + "maven {\n" + indentation + "    name '" + repoType.getName() + "'\n" + indentation + "    url '" + repoType.getRepoBase() + "'\n" + indentation + "}";
    }
}

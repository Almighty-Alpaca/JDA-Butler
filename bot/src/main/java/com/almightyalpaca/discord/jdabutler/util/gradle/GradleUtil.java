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

    public static String getBuildFile(final boolean kotlin, final Collection<Pair<String, String>> plugins, final String mainClassName, final String version, final String sourceCompatibility, final List<VersionedItem> items, final boolean pretty)
    {
        char quote = kotlin ? '"' : '\'';
        String base = GradleUtil.getPluginsBlock(kotlin, plugins) +
                "\n\nmainClassName = " + quote + mainClassName + quote +
                "\nversion " + quote + version + quote +
                "\nsourceCompatibility = " + sourceCompatibility +
                "\n\n" +
                GradleUtil.getRepositoryBlock(kotlin, items) +
                "\n\n" +
                GradleUtil.getDependencyBlock(kotlin, items, pretty) +
                "\n\n";
        if(kotlin) {
            return base + "tasks.withType<JavaCompile> {\n    options.encoding = \"UTF-8\"\n    options.isIncremental = true\n}\n";
        } else {
            return base + "compileJava.options.encoding = 'UTF-8'\n";
        }
    }

    public static String getDependencyBlock(final boolean kotlin, final List<VersionedItem> items, final boolean pretty)
    {
        StringBuilder text = new StringBuilder("dependencies {\n");
        for (final VersionedItem item : items)
            text.append("    ").append(GradleUtil.getDependencyString(kotlin, item, pretty)).append("\n");
        text.append("}");
        return text.toString();
    }

    public static String getDependencyString(final boolean kotlin, final VersionedItem item, final boolean pretty)
    {
        if(kotlin) {
            return String.format("implementation(\"%s:%s:%s\")", item.getGroupId(), item.getArtifactId(), item.getVersion());
        } else {
            if (pretty)
                return String.format("implementation group: '%s', name: '%s', version: '%s'",
                        item.getGroupId(), item.getArtifactId(), item.getVersion());
            else
                return String.format("implementation '%s:%s:%s'", item.getGroupId(), item.getArtifactId(), item.getVersion());
        }
    }

    public static String getPluginsBlock(final boolean kotlin, final Collection<Pair<String, String>> plugins)
    {
        StringBuilder text = new StringBuilder("plugins {\n");
        String indentation = "    ";
        for (final Pair<String, String> plugin : plugins)
        {
            if(kotlin) {
                text.append(indentation);
                if(plugin.getRight() != null) {
                    text.append("id(\"").append(plugin.getLeft()).append("\") version \"").append(plugin.getRight()).append('"');
                } else {
                    text.append(plugin.getLeft());
                }
            } else {
                text.append(indentation).append("id '").append(plugin.getLeft()).append("'");
                if (plugin.getRight() != null)
                    text.append(" version '").append(plugin.getRight()).append("'");
            }
            text.append("\n");
        }
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryBlock(final boolean kotlin, final List<VersionedItem> items)
    {
        StringBuilder text = new StringBuilder("repositories {\n");
        items.stream()
                .filter(item -> item.getRepoType() != null)
                .flatMap(item -> item.getAllRepositories().stream())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(repoType ->
                        text.append(GradleUtil.getRepositoryString(kotlin, repoType, "    ")).append("\n")
                );
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryString(final boolean kotlin, final RepoType repoType, String indentation)
    {
        if (indentation == null)
            indentation = "";
        if (repoType.getGradleName() != null)
            return indentation + repoType.getGradleName() + "()";
        else if (kotlin)
            return indentation + "maven(\"" + repoType.getRepoBase() + "\")";
        else
            return indentation + "maven {\n" + indentation + "    name '" + repoType.getName() + "'\n" + indentation + "    url '" + repoType.getRepoBase() + "'\n" + indentation + "}";
    }
}

package com.almightyalpaca.discord.jdabutler;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Arrays;
import java.util.Collection;

public class GradleUtil
{

    public static final Collection<Pair<String, String>> DEFAULT_PLUGINS = Arrays.asList(new ImmutablePair<>("java", null), new ImmutablePair<>("application", null), new ImmutablePair<>("com.github.johnrengelman.shadow", GradleUtil.SHADOW_VERSION));

    public static final String SHADOW_VERSION = "2.0.1";

    public static String getBuildFile(final Collection<Pair<String, String>> plugins, final String mainClassName, final String version, final String sourceCompatibility, final Collection<Triple<String, String, String>> dependencies, final Collection<Pair<String, String>> repositories, final boolean pretty)
    {
        String text;

        text = GradleUtil.getPluginsBlock(plugins) + "\n";
        text += "\n";
        text += "mainClassName = '" + mainClassName + "'\n";
        text += "\n";
        text += "version '" + version + "'\n";
        text += "\n";
        text += "sourceCompatibility = " + sourceCompatibility + "\n";
        text += "\n";
        text += GradleUtil.getRepositoryBlock(repositories) + "\n";
        text += "\n";
        text += GradleUtil.getDependencyBlock(dependencies, pretty) + "\n";
        text += "\n";
        text += "compileJava.options.encoding = 'UTF-8'" + "\n";

        return text;
    }

    public static String getDependencyBlock(final Collection<Triple<String, String, String>> dependencies, final boolean pretty)
    {
        StringBuilder text = new StringBuilder("dependencies {\n");
        for (final Triple<String, String, String> dependency : dependencies)
            text.append("    ").append(GradleUtil.getDependencyString(dependency.getLeft(), dependency.getMiddle(), dependency.getRight(), pretty)).append("\n");
        text.append("}");
        return text.toString();
    }

    public static String getDependencyString(final String group, final String name, final String version, final boolean pretty)
    {
        if (pretty)
            return "compile group: '" + group + "', name: '" + name + "', version: '" + version + "'";
        else
            return "compile '" + group + ":" + name + ":" + version + "'";
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

    public static String getRepositoryBlock(final Collection<Pair<String, String>> repositories)
    {
        StringBuilder text = new StringBuilder("repositories {\n");
        for (final Pair<String, String> repository : repositories)
            text.append(GradleUtil.getRepositoryString(repository.getLeft(), repository.getRight(), "    ")).append("\n");
        text.append("}");
        return text.toString();
    }

    public static String getRepositoryString(final String name, final String url, String indentation)
    {
        if (indentation == null)
            indentation = "";
        if (name.equals("jcenter()") && url == null)
            return indentation + name;
        else if (name.equals("mavenCentral()") && url == null)
            return indentation + name;
        else
            return indentation + "maven {\n" + indentation + "    name '" + name + "'\n" + indentation + "    url '" + url + "'\n" + indentation + "}";
    }
}

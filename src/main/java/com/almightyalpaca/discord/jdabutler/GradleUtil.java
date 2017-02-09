package com.almightyalpaca.discord.jdabutler;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class GradleUtil {

	public static final String								SHADOW_VERSION	= "1.2.4";

	public static final Collection<Pair<String, String>>	DEFAULT_PLUGINS	= Arrays.asList(new ImmutablePair<>("java", null), new ImmutablePair<>("application", null), new ImmutablePair<>(
			"com.github.johnrengelman.shadow", GradleUtil.SHADOW_VERSION));

	public static String getBuildFile(final Collection<Pair<String, String>> plugins, final String mainClassName, final String version, final String sourceCompatibility,
			final Collection<Triple<String, String, String>> dependencies, final Collection<Pair<String, String>> repositories, final boolean pretty) {
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

	public static String getDependencyBlock(final Collection<Triple<String, String, String>> dependencies, final boolean pretty) {
		String text = "dependencies {\n";
		for (final Triple<String, String, String> dependency : dependencies) {
			text += "    " + GradleUtil.getDependencyString(dependency.getLeft(), dependency.getMiddle(), dependency.getRight(), pretty) + "\n";
		}
		text += "}";
		return text;
	}

	public static String getDependencyString(final String group, final String name, final String version, final boolean pretty) {
		if (pretty)
			return "compile group: '" + group + "', name: '" + name + "', version: '" + version + "'";
		else
			return "compile '" + group + ":" + name + ":" + version + "'";
	}

	public static String getPluginsBlock(final Collection<Pair<String, String>> plugins) {
		String text = "plugins {\n";
		for (final Pair<String, String> plugin : plugins) {
			text += "    id'" + plugin.getLeft() + "'";
			if (plugin.getRight() != null) {
				text += " version '" + plugin.getRight() + "'";
			}
			text += "\n";
		}
		text += "}";
		return text;
	}

	public static String getRepositoryBlock(final Collection<Pair<String, String>> repositories) {
		String text = "repositories {\n";
		for (final Pair<String, String> repository : repositories) {
			text += GradleUtil.getRepositoryString(repository.getLeft(), repository.getRight(), "    ") + "\n";
		}
		text += "}";
		return text;
	}

	public static String getRepositoryString(final String name, final String url, String indentation) {
		if (indentation == null) {
			indentation = "";
		}
		if (name.equals("jcenter()") && url == null)
			return indentation + name;
		else if (name.equals("mavenCentral()") && url == null)
			return indentation + name;
		else
			return indentation + "maven {\n" + indentation + "    name '" + name + "'\n" + indentation + "    url '" + url + "'\n" + indentation + "}";
	}
}

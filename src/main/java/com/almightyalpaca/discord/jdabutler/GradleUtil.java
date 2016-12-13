package com.almightyalpaca.discord.jdabutler;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class GradleUtil {
	public static String getBuildFile(final Collection<Triple<String, String, String>> dependencies, final Collection<Pair<String, String>> repositories, final boolean pretty) {
		final String text = "plugins {\n    id 'java'\n    id 'application'\n    id 'com.github.johnrengelman.shadow' version '1.2.4'\n}\n// Change this to the name of your main class\nmainClassName = 'com.example.jda.Bot'\n\n// This version will be appeneded to the jar name\nversion '1.0'\ngroup 'com.mydomain.example'\n\n// The java version\nsourceCompatibility = 1.8\ntargetCompatibility = 1.8\n\n"
				+ GradleUtil.getRepositoryBlock(repositories) + "\n\n// Add your dependencies here\n" + GradleUtil.getDependencyBlock(dependencies, pretty)
				+ "\n\n// Tell gradle to use UTF-8 as file encoding\ncompileJava.options.encoding = 'UTF-8'\n\n// This task is to set the gradle wrapper version\nwrapper {\n    gradleVersion = '3.2.1'\n}";
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
		if (pretty) {
			return "compile group: '" + group + "', name: '" + name + "', version: '" + version + "'";
		} else {
			return "compile '" + group + ":" + name + ":" + version + "'";
		}
	}

	public static String getRepositoryBlock(final Collection<Pair<String, String>> repositories) {
		String text = "repositories {\n";
		for (final Pair<String, String> repository : repositories) {
			text += GradleUtil.getRepositoryString(repository.getLeft(), repository.getRight(), "    ") + "\n";
		}
		text += "}";
		return text;
	}

	private static String getRepositoryString(final String name, final String url, String indentation) {
		if (indentation == null) {
			indentation = "";
		}
		if (name.equals("jcenter()") && url == null) {
			return indentation + name;
		} else if (name.equals("mavenCentral()") && url == null) {
			return indentation + name;
		} else {
			return indentation + "maven {\n" + indentation + "    name '" + name + "'\n" + indentation + "    url '" + url + "'\n" + indentation + "}";
		}
	}

}

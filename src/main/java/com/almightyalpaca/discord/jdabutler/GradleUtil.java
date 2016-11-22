package com.almightyalpaca.discord.jdabutler;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Triple;

public class GradleUtil {
	public static String getBuildFile(final Collection<Triple<String, String, String>> dependencies, final boolean pretty) {
		String text = "plugins {\r\n    id 'java'\r\n    id 'application'\r\n    id 'com.github.johnrengelman.shadow' version '1.2.4'\r\n}\r\n// Change this to the name of your main class\r\nmainClassName = 'com.example.jda.Bot'\r\n\r\n// This version will be appeneded to the jar name\r\nversion '1.0'\r\n\r\ngroup 'com.mydomain.example'\r\n\r\n// The java version\r\nsourceCompatibility = 1.8\r\ntargetCompatibility = 1.8\r\n\r\nrepositories {\r\n    mavenCentral()\r\n    jcenter()\r\n}\r\n\r\n// Add your dependencies here\r\ndependencies {\r\n";
		for (final Triple<String, String, String> dependency : dependencies) {
			text += "    " + GradleUtil.getDependencyString(dependency.getLeft(), dependency.getMiddle(), dependency.getRight(), pretty) + "'\r\n";
		}

		text += "}\r\n\r\n// Tell gradle to use UTF-8 as file encoding\r\ncompileJava.options.encoding = 'UTF-8'\r\n\r\n// This task is to set the gradle wrapper version\r\ntask wrapper(type: Wrapper) {\r\n    gradleVersion = '3.2'\r\n}";
		return text;
	}

	public static String getDependencyString(final String group, final String name, final String version, final boolean pretty) {
		if (pretty) {
			return "compile group: '" + group + "', name: '" + name + "', version: '" + version + "'";
		} else {
			return "compile '" + group + ":" + name + ":" + version + "'";
		}
	}
}

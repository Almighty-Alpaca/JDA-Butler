package com.almightyalpaca.discord.jdabutler;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Triple;

public class GradleUtil {
	public static String getBuildFile(Collection<Triple<String, String, String>> dependencies, boolean pretty) {
		String text = "plugins {\r\n    id 'java'\r\n    id 'application'\r\n    id 'com.github.johnrengelman.shadow' version '1.2.4'\r\n}\r\n// Change this to the name of your main class\r\nmainClassName = 'com.example.jda.Bot'\r\n\r\n// This version will be appeneded to the jar name\r\nversion '1.0'\r\n\r\ngroup 'com.mydomain.example'\r\n\r\n// The java version\r\nsourceCompatibility = 1.8\r\ntargetCompatibility = 1.8\r\n\r\nrepositories {\r\n    mavenCentral()\r\n    jcenter()\r\n}\r\n\r\n// Add your dependencies here\r\ndependencies {\r\n";

		if (pretty) {
			for (Triple<String, String, String> dependency : dependencies) {
				text += "    compile group: '" + dependency.getLeft() + "', name: '" + dependency.getMiddle() + "', version: '" + dependency.getRight() + "'\r\n";
			}
		} else {
			for (Triple<String, String, String> dependency : dependencies) {
				text += "    compile '" + dependency.getLeft() + ":" + dependency.getMiddle() + ":" + dependency.getRight() + "'\r\n";
			}
		}

		text += "}\r\n\r\n// Tell gradle to use UTF-8 as file encoding\r\ncompileJava.options.encoding = 'UTF-8'\r\n\r\n// This task is to set the gradle wrapper version\r\ntask wrapper(type: Wrapper) {\r\n    gradleVersion = '3.1'\r\n}";
		return text;

	}
}

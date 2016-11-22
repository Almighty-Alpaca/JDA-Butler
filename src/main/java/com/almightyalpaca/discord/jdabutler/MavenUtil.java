package com.almightyalpaca.discord.jdabutler;

public class MavenUtil {
	public static String getDependencyString(final String group, final String name, final String version, String indentation) {
		if (indentation == null) {
			indentation = "";
		}

		return indentation + "<dependency>\n" + indentation + "    <groupId>" + group + "</groupId>\n" + indentation + "    <artifactId>" + name + "</artifactId>\n" + indentation + "    <version>"
				+ version + "</version>\n" + indentation + "</dependency>\n";
	}
}

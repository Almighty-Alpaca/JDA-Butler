package com.almightyalpaca.discord.jdabutler;

public class MavenUtil {
	public static String getDependencyString(final String group, final String name, final String version, String indentation) {
		if (indentation == null) {
			indentation = "";
		}

		return indentation + "<dependency>\n" + indentation + "    <groupId>" + group + "</groupId>\n" + indentation + "    <artifactId>" + name + "</artifactId>\n" + indentation + "    <version>"
				+ version + "</version>\n" + indentation + "</dependency>\n";
	}

	public static String getRepositoryString(final String id, final String name, final String url, String indentation) {
		if (indentation == null) {
			indentation = "";
		}
		return indentation + "<repository>\n" + indentation + "    <id>" + id + "</id>\n" + indentation + "    <name>" + name + "</name>\n" + indentation + "    <url>" + url + "</url>\n" + indentation
				+ "</repository>\n";
	}

}

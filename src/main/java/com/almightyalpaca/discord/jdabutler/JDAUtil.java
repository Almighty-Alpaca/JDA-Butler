package com.almightyalpaca.discord.jdabutler;

public class JDAUtil {
	public static int getBuildNumber(String build) {
		try {
			int lastIndexOfUnderscore = build.lastIndexOf('_');
			if (lastIndexOfUnderscore != -1) {
				build = build.substring(lastIndexOfUnderscore + 1);
			}

			return Integer.parseInt(build);
		} catch (Exception e) {
			throw new NumberFormatException("Invalid build number");
		}
	}
}
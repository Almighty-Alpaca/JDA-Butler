package com.almightyalpaca.discord.jdabutler;

public class BotVersion
{
    public static final String BUILD = "@BUILD@";
    public static final String GIT_INFO = "@VERSION@";

    private static final String[] GIT_LINES = GIT_INFO.split("\n");

    public static final String GIT_REF = GIT_LINES.length == 1 ? "" : GIT_LINES[0];
    public static final String GIT_COMMIT = GIT_LINES.length == 1 ? "" : GIT_LINES[1];
    public static final String GIT_MSG = GIT_LINES.length == 1 ? "" : GIT_LINES[2];

    public static final String FULL_VERSION = String.format("Build: %s\n\nGit:\n%s", BUILD, GIT_INFO);
}

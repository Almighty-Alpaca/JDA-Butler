package com.almightyalpaca.discord.jdabutler;

public class BotVersion
{
    public static final String FULL_VERSION = "@VERSION@";

    private static final String[] VERSION_LINES = FULL_VERSION.split("\n");

    public static final String VERSION_REF = VERSION_LINES.length == 1 ? "" : VERSION_LINES[0];
    public static final String VERSION_COMMIT = VERSION_LINES.length == 1 ? "" : VERSION_LINES[1];
    public static final String VERSION_MSG = VERSION_LINES.length == 1 ? "" : VERSION_LINES[2];
}

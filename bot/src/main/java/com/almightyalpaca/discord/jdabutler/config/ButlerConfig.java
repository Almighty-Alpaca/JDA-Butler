package com.almightyalpaca.discord.jdabutler.config;

public interface ButlerConfig {

    // Webhook settings
    boolean webhookEnabled();
    String webhookLevel();
    String webhookPattern();
    String webhookUrl();

    // JDA configuration
    long guildId();
    long botRoleId();
    long staffRoleId();
    long helperRoleId();
    String botToken();

    // Api configs
    String dropboxAccessToken();
    String githubToken();

    // JDA Version settings
    int jdaVersionBuild();
    String jdaVersionName();

    // dummy method for backwards compatibility
    default void save() {}
}

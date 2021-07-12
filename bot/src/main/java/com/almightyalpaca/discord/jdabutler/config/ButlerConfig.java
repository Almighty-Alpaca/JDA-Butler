package com.almightyalpaca.discord.jdabutler.config;

public interface ButlerConfig {

    // General settings
    boolean testing();
    String prefix();

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
    boolean blacklistEnabled();

    // Api configs
    String dropboxAccessToken();
    String githubToken();

    // JDA Version settings
    int jdaVersionBuild();
    String jdaVersionName();
    void setJDAVersionBuild(int newVersion);
    void setJDAVersionName(String newName);

    // Used to save the results of setJDAVersionBuild and setJDAVersionName
    void save();
}

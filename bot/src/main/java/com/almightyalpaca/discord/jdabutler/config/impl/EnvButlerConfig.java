package com.almightyalpaca.discord.jdabutler.config.impl;

import com.almightyalpaca.discord.jdabutler.config.ButlerConfig;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static java.lang.System.getenv;

public class EnvButlerConfig implements ButlerConfig {

    @Override
    public boolean testing() {
        return parseBoolean(getenv("BUTLER_TESTING"));
    }

    @Override
    public String prefix() {
        return getenv("BUTLER_PREFIX");
    }

    @Override
    public boolean webhookEnabled() {
        return parseBoolean(getenv("BUTLER_WEBHOOK_ENABLED"));
    }

    @Override
    public String webhookLevel() {
        return getenv("BUTLER_WEBHOOK_LEVEL");
    }

    @Override
    public String webhookPattern() {
        return getenv("BUTLER_WEBHOOK_PATTERN");
    }

    @Override
    public String webhookUrl() {
        return getenv("BUTLER_WEBHOOK_URL");
    }

    @Override
    public long guildId() {
        return parseLong(getenv("BUTLER_GUILD_ID"));
    }

    @Override
    public long botRoleId() {
        return parseLong(getenv("BUTLER_BOT_ROLE_ID"));
    }

    @Override
    public long staffRoleId() {
        return parseLong(getenv("BUTLER_STAFF_ROLE_ID"));
    }

    @Override
    public long helperRoleId() {
        return parseLong(getenv("BUTLER_HELPER_ROLE_ID"));
    }

    @Override
    public String botToken() {
        return getenv("BUTLER_BOT_TOKEN");
    }

    @Override
    public boolean blacklistEnabled() {
        return parseBoolean(getenv("BUTLER_BLACKLIST_ENABLED"));
    }

    @Override
    public String dropboxAccessToken() {
        return getenv("BUTLER_DROPBOX_TOKEN");
    }

    @Override
    public String githubToken() {
        return getenv("BUTLER_GITHUB_TOKEN");
    }

    // will use file based storage
    @Override
    public int jdaVersionBuild() {
        return 0;
    }

    @Override
    public String jdaVersionName() {
        return null;
    }

    @Override
    public void setJDAVersionBuild(int newVersion) {

    }

    @Override
    public void setJDAVersionName(String newName) {

    }

    @Override
    public void save() {

    }
}

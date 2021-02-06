package com.almightyalpaca.discord.jdabutler.config.impl;

import com.almightyalpaca.discord.jdabutler.config.ButlerConfig;
import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;

import java.io.File;
import java.io.IOException;

public class JsonConfigImpl implements ButlerConfig {

    private final Config config;

    public JsonConfigImpl() throws IOException {
        this.config = ConfigFactory.getConfig(new File("config.json"));
    }

    @Override
    public boolean webhookEnabled() {
        return this.config.getBoolean("webhook.enabled", false);
    }

    @Override
    public String webhookLevel() {
        return this.config.getString("webhook.level");
    }

    @Override
    public String webhookPattern() {
        return this.config.getString("webhook.pattern");
    }

    @Override
    public String webhookUrl() {
        return this.config.getString("webhook.webhookurl");
    }

    @Override
    public long guildId() {
        return this.config.getLong("discord.guild_id", 125227483518861312L);
    }

    @Override
    public long botRoleId() {
        return this.config.getLong("discord.bot_role_id", 125616720156033024L);
    }

    @Override
    public long staffRoleId() {
        return this.config.getLong("discord.staff_role_id", 169481978268090369L);
    }

    @Override
    public long helperRoleId() {
        return this.config.getLong("discord.helper_role_id", 183963327033114624L);
    }

    @Override
    public String botToken() {
        return this.config.getString("discord.token", "Your token");
    }

    @Override
    public String dropboxAccessToken() {
        return null;
    }

    @Override
    public String githubToken() {
        return null;
    }

    @Override
    public int jdaVersionBuild() {
        return this.config.getInt("jda.version.build");
    }

    @Override
    public String jdaVersionName() {
        return null;
    }

    @Override
    public void save() {
        this.config.save();
    }
}

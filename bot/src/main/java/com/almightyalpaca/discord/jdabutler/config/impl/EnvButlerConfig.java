package com.almightyalpaca.discord.jdabutler.config.impl;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.config.ButlerConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static java.lang.System.getenv;

public class EnvButlerConfig implements ButlerConfig {

    private static final String VERSION_BUILD_FILE = "jdaVersionBuildNum.txt";
    private static final String VERSION_NAME_FILE = "jdaVersionName.txt";

    private final AtomicInteger jdaVersionBuild;
    private final AtomicReference<String> jdaVersionName;

    public EnvButlerConfig() {
        // init version objects
        final String tmpVersionNum = readVersionFile(VERSION_BUILD_FILE, "-1");
        final String tmpVersionName = readVersionFile(VERSION_NAME_FILE, "");

        this.jdaVersionBuild = new AtomicInteger(Integer.parseInt(tmpVersionNum));
        this.jdaVersionName = new AtomicReference<>(tmpVersionName);
    }

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

    @Override
    public int jdaVersionBuild() {
        return this.jdaVersionBuild.get();
    }

    @Override
    public String jdaVersionName() {
        return this.jdaVersionName.get();
    }

    @Override
    public void setJDAVersionBuild(int newVersion) {
        this.jdaVersionBuild.set(newVersion);
    }

    @Override
    public void setJDAVersionName(String newName) {
        this.jdaVersionName.set(newName);
    }

    @Override
    public void save() {
        writeVersionFile(VERSION_BUILD_FILE, this.jdaVersionBuild.toString()); // runs Integer.toString(get())
        writeVersionFile(VERSION_NAME_FILE, this.jdaVersionName.get());
    }

    private String readVersionFile(String file, String defaultVal) {
        try {
            final File versionFile = new File(file);

            if (!versionFile.exists()) {
                versionFile.createNewFile();

                writeVersionFile(file, defaultVal);

                return defaultVal;
            }

            return String.join("", Files.readAllLines(versionFile.toPath())).trim();
        } catch(IOException e) {
            Bot.LOG.error("Version file operation failed", e);
        }

        return defaultVal;
    }

    // file must exist
    private void writeVersionFile(String file, String value) {
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(value);
        } catch(IOException e) {
            Bot.LOG.error("Error writing to file", e);
        }
    }
}

package com.kantenkugel.discordbot.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Launcher {
    private static final String[] JAVA_ARGS = { "-Xmx250m" };

    private static final int UPDATE_RETURN_VALUE = 101;

    private static final Path BOT_FILE = Paths.get("Bot-all.jar");
    private static final Path BOT_UPDATE = Paths.get("Bot_Update.jar");

    private static final long SLEEP_TIMEOUT = 1000;
    private static final int MAX_ATTEMPTS = 10;

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        if(!Files.exists(BOT_FILE)) {
            LOG.warn("Bot file not found. aborting ({})", BOT_FILE.toAbsolutePath().toString());
            return;
        }

        if(!Files.isWritable(BOT_FILE)) {
            LOG.warn("Bot might still be running. aborting");
            return;
        }

        if(Files.exists(BOT_UPDATE)) {
            //update first
            if(!update())
                LOG.warn("Found update file but could not update... starting up anyway");
        }

        int returnValue;
        do {
            Process p = startBot();
            if(p == null)
                return;
            try {
                returnValue = p.waitFor();
                if(returnValue == UPDATE_RETURN_VALUE && Files.exists(BOT_UPDATE)) {
                    if(!update())
                        return;
                }
            } catch(InterruptedException e) {
                LOG.error("Could not wait for bot to exit", e);
                return;
            }
        } while(returnValue == UPDATE_RETURN_VALUE);
        LOG.info("Bot shut down with code {}", returnValue);
    }

    private static boolean update() {
        LOG.info("Updating Bot after file is free for writing");
        return update(0);
    }

    private static boolean update(int attempt) {
        if(attempt >= MAX_ATTEMPTS) {
            LOG.error("Maximum attempts to update reached. aborting");
            return false;
        }
        try {
            LOG.debug("Waiting one second for bot to fully close");
            Thread.sleep(SLEEP_TIMEOUT);
            if(!Files.isWritable(BOT_FILE)) {
                LOG.debug("Bot file not yet writable, trying later");
                return update(attempt + 1);
            }
            LOG.debug("Moving update file to bot file");
            Files.deleteIfExists(BOT_FILE);
            Files.move(BOT_UPDATE, BOT_FILE);
            return true;
        } catch(IOException | InterruptedException e) {
            LOG.error("There was an error moving the update file", e);
        }
        return false;
    }

    private static Process startBot() {
        try {
            LOG.info("Starting bot...");
            return new ProcessBuilder(getStartCommand()).inheritIO().start();
        } catch(IOException e) {
            LOG.error("Starting bot failed, used start command {}", getStartCommand(), e);
        }
        return null;
    }

    private static List<String> getStartCommand() {
        List<String> command = new ArrayList<>(JAVA_ARGS.length+2);
        command.add("java");
        command.addAll(Arrays.asList(JAVA_ARGS));
        command.add("-jar");
        command.add(BOT_FILE.toString());
        return command;
    }
}

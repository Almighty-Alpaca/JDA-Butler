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

public class Main {
    private static final String[] JAVA_ARGS = { "-Xmx175M" };

    private static final Path BOT_FILE = Paths.get("Bot.jar");
    private static final Path BOT_UPDATE = Paths.get("Bot_Update.jar");

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if(!Files.exists(BOT_UPDATE)) {
            //just try launching bot
            LOG.info("No update file exists. Just attempting to start bot normally...");
            startBot();
        } else {
            //update
            try {
                LOG.info("Updating bot... waiting for System.in to close...");
                System.in.read(); //blocks until input (either user input or -1 if System.in was closed)
                    update();
            } catch(IOException e) {
                LOG.error("IO Error occurred, waiting for System.in to close. Trying to update anyway... ", e);
                update();
            }
        }
    }

    private static void update() {
        try {
            LOG.debug("Waiting one second for bot to fully close");
            Thread.sleep(1000);
            LOG.debug("Moving update file to bot file");
            Files.deleteIfExists(BOT_FILE);
            Files.move(BOT_UPDATE, BOT_FILE);
            startBot();
        } catch(IOException | InterruptedException e) {
            LOG.error("There was an error moving the update file", e);
        }
    }

    private static void startBot() {
        try {
            LOG.info("Starting bot...");
            new ProcessBuilder(getStartCommand()).inheritIO().start();
        } catch(IOException e) {
            LOG.error("Starting bot failed, used start command {}", getStartCommand(), e);
        }
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

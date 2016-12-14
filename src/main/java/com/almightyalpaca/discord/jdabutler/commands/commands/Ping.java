package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.temporal.ChronoUnit;

public class Ping implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        channel.sendMessage("Ping: ...")
                .queue(m -> m.editMessage("Ping: " + message.getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS) + "ms").queue());
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getHelp() {
        return "Pong";
    }
}

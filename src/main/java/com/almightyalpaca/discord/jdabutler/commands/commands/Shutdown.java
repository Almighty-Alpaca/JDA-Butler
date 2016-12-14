package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class Shutdown implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        if (Bot.isAdmin(sender))
            Bot.shutdown();
    }

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getHelp() {
        return null;
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.moduleutils.DocParser;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class DocsCommand implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        MessageBuilder mb = new MessageBuilder();
        mb.append(DocParser.get(content));
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getName() {
        return "docs";
    }

    @Override
    public String getHelp() {
        return "Displays documentation";
    }
}

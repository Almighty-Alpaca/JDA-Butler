package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class HelpCommand implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        EmbedBuilder builder = new EmbedBuilder();
        String help = Bot.dispatcher.getCommands().stream()
                .map(c -> String.format("%s - %s", c.getName().toLowerCase(), c.getHelp()))
                .collect(Collectors.joining("\n"));
        builder.setAuthor("Help Command", null, Bot.jda.getSelfUser().getEffectiveAvatarUrl());
        builder.setDescription(help);
        builder.setFooter(String.format("Requested on %s by %s",
                OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME), sender.getName()),
                Bot.jda.getSelfUser().getEffectiveAvatarUrl());
        channel.sendMessage(new MessageBuilder().setEmbed(builder.build()).build()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp() {
        return "Prints a list of commands";
    }
}

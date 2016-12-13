package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class VersionsCommand implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);
        eb.setAuthor("Latest versions", null, EmbedUtil.JDA_ICON);
        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE);
        eb.addField("JDA", "[" + Bot.config.getString("jda.version.name") + "](http://home.dv8tion.net:8080/job/JDA/lastSuccessfulBuild/)", true);
        eb.addField("Lavaplayer", "[" + Lavaplayer.getLatestVersion() + "](https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord)", true);
        channel.sendMessage(new MessageBuilder().setEmbed(eb.build()).build()).queue();
    }

    @Override
    public String getName() {
        return "versions";
    }

    @Override
    public String getHelp() {
        return "Prints versions of all the things that matter :D";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"latest"};
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class Jar implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content) {
        MessageBuilder mb = new MessageBuilder();
        final String version = Bot.config.getString("jda.version.name");
        final int build = Bot.config.getInt("jda.version.build");
        mb.append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-javadoc.jar").append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build
                + "/artifact/build/libs/JDA-" + version + "-sources.jar").append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + ".jar")
                .append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-withDependencies-" + version + ".jar");
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getName() {
        return "jar";
    }

    @Override
    public String getHelp() {
        return "Displays links to all JAR files";
    }
}

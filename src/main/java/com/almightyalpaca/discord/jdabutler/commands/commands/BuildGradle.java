package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collection;

public class BuildGradle implements Command {
    @Override
    public void dispatch(String[] args, User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event) {
        MessageBuilder mb = new MessageBuilder();

        final boolean lavaplayer = ArrayUtils.contains(args, "lavaplayer");
        final boolean pretty = ArrayUtils.contains(args, "pretty");

        final Collection<Pair<String, String>> repositories = new ArrayList<>(2);
        final Collection<Triple<String, String, String>> dependencies = new ArrayList<>(2);

        dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
        repositories.add(new ImmutablePair<>("jcenter()", null));

        if (lavaplayer) {
            dependencies.add(new ImmutableTriple<>(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion()));
            repositories.add(new ImmutablePair<>(Lavaplayer.REPO_NAME, Lavaplayer.REPO_URL));
        }

        mb.appendCodeBlock(GradleUtil.getBuildFile(dependencies, repositories, pretty), "gradle");
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getName() {
        return "build.gradle";
    }

    @Override
    public String getHelp() {
        return "Shows an example build.gradle file";
    }
}

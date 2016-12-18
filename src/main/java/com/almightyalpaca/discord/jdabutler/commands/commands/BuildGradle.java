package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class BuildGradle implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		final MessageBuilder mb = new MessageBuilder();

		final boolean lavaplayer = content.contains("player");
		final boolean pretty = content.contains("pretty");

		final Collection<Pair<String, String>> repositories = new ArrayList<>(2);
		final Collection<Triple<String, String, String>> dependencies = new ArrayList<>(2);

		dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
		repositories.add(new ImmutablePair<>("jcenter()", null));

		if (lavaplayer) {
			dependencies.add(new ImmutableTriple<>(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion()));
			repositories.add(new ImmutablePair<>(Lavaplayer.REPO_NAME, Lavaplayer.REPO_URL));
		}

		mb.appendCodeBlock(GradleUtil.getBuildFile(GradleUtil.DEFAULT_PLUGINS, "com.example.jda.Bot", "1.0", "1.8", dependencies, repositories, pretty), "gradle");
		channel.sendMessage(mb.build()).queue();
	}

	@Override
	public String getHelp() {
		return "Shows an example build.gradle file";
	}

	@Override
	public String getName() {
		return "build.gradle";
	}
}

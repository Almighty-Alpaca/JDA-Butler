package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class GradleCommand implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		final MessageBuilder mb = new MessageBuilder();
		final EmbedBuilder eb = new EmbedBuilder();

		final boolean lavaplayer = content.contains("player");
		final boolean pretty = content.contains("pretty");

		String author = "Gradle dependencies for JDA";
		if (lavaplayer) {
			author += " and Lavaplayer";
		}

		eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

		String field = "If you don't know gradle type `!build.gradle` for a complete gradle build file\n\n```gradle\n";

		final Collection<Pair<String, String>> repositories = new ArrayList<>(2);
		final Collection<Triple<String, String, String>> dependencies = new ArrayList<>(2);

		dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
		repositories.add(new ImmutablePair<>("jcenter()", null));

		if (lavaplayer) {
			dependencies.add(new ImmutableTriple<>(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion()));
		}

		field += GradleUtil.getDependencyBlock(dependencies, pretty) + "\n";
		field += "\n";

		field += GradleUtil.getRepositoryBlock(repositories) + "\n";

		field += "```";

		eb.addField("", field, false);

		EmbedUtil.setColor(eb);
		mb.setEmbed(eb.build());
		channel.sendMessage(mb.build()).queue();
	}

	@Override
	public String getHelp() {
		return "Shows the gradle `compile ...` line";
	}

	@Override
	public String getName() {
		return "gradle";
	}
}

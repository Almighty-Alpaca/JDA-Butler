package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Maven implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		final MessageBuilder mb = new MessageBuilder();
		final EmbedBuilder eb = new EmbedBuilder();

		final boolean lavaplayer = content.contains("player");

		String author = "Maven dependencies for JDA";
		if (lavaplayer) {
			author += " and Lavaplayer";
		}

		eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

		String field = "If you don't know maven type `!pom.xml` for a complete maven build file \n\n```xml\n";

		field += MavenUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda.version.name"), null) + "\n";
		if (lavaplayer) {
			field += MavenUtil.getDependencyString(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion(), null) + "\n";
		}

		field += "\n";

		field += MavenUtil.getRepositoryString("jcenter", "jcenter-bintray", "http://jcenter.bintray.com", null) + "\n";

		if (lavaplayer) {
			field += MavenUtil.getRepositoryString("sedmelluq", "sedmelluq", "http://maven.sedmelluq.com/", null) + "\n";
		}

		field += "```";

		eb.addField("", field, false);

		EmbedUtil.setColor(eb);
		mb.setEmbed(eb.build());
		channel.sendMessage(mb.build()).queue();
	}

	@Override
	public String getHelp() {
		return "Shows maven dependency information";
	}

	@Override
	public String getName() {
		return "maven";
	}
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.kantenkugel.discordbot.moduleutils.DocParser;

import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class DocsCommand implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		final MessageBuilder mb = new MessageBuilder();
		mb.append(DocParser.get(content));
		channel.sendMessage(mb.build()).queue();
	}

	@Override
	public String getHelp() {
		return "Displays documentation";
	}

	@Override
	public String getName() {
		return "docs";
	}
}

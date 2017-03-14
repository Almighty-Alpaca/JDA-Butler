package com.almightyalpaca.discord.jdabutler.commands;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Command {
	void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event) throws Exception;

	default String[] getAliases() {
		return new String[0];
	}

	String getHelp();

	String getName();

	default void sendFailed(final Message message) {
		message.addReaction("❌").queue();
	}

}

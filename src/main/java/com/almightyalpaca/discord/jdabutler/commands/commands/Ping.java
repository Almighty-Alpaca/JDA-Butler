package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.time.temporal.ChronoUnit;

import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Ping implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		channel.sendMessage("Ping: ...").queue(m -> m.editMessage("Ping: " + message.getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS) + "ms").queue());
	}

	@Override
	public String getHelp() {
		return "Pong";
	}

	@Override
	public String getName() {
		return "ping";
	}
}

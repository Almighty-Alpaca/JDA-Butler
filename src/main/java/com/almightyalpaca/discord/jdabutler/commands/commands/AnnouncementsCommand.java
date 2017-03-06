package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class AnnouncementsCommand implements Command {
	@Override
	public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event) throws Exception {
		if(!Bot.isHelper(sender)){
			channel.sendMessage("Helper+ only command!").queue();
			return;
		}
		if (content.toLowerCase().matches(".+ .+")) {
			String[] args = content.split(" ", 2);
			List<Role> roles = channel.getGuild().getRolesByName(args[0] + " updates", true);
			if (roles.isEmpty()) {
				channel.sendMessage("Found no roles to mention!").queue();
			} else {
				Role role = roles.get(0);
				role.getManager().setMentionable(true)
						.queue(s -> channel.sendMessage(role.getAsMention() + " " + args[1])
								.queue(m -> role.getManager().setMentionable(false).queue()));
			}
		}
	}

	@Override
	public String getHelp() {
		return "`announce [type] [text]` - Mentions `@[type] Updates` role with the text following.";
	}

	@Override
	public String getName() {
		return "announce";
	}
}

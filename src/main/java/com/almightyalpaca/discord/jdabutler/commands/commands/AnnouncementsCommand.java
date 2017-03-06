package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnouncementsCommand implements Command {
	private static final Map<String, String> CHANNEL_TYPES;

	static {
		CHANNEL_TYPES = new HashMap<>();
		CHANNEL_TYPES.put("263484072389640193", "lavaplayer");
		CHANNEL_TYPES.put("125227483518861312", "jda");
	}

	@Override
	public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event) throws Exception {
		if (!Bot.isHelper(sender)) {
			channel.sendMessage("Helper+ only command!").queue();
			return;
		}
		if (content.toLowerCase().matches(".+ .+")) {
			String[] args = content.split(" ", 2);
			if (!CHANNEL_TYPES.get(channel.getId()).contains(args[0].toLowerCase())) {
				channel.sendMessage("https://i.arsenarsen.com/gkqgz2rz31.png").queue();
				return;
			}
			List<Role> roles = channel.getGuild().getRolesByName(args[0] + " updates", true);
			if (roles.isEmpty()) {
				channel.sendMessage("Found no roles to mention!").queue();
			} else {
				message.delete().queue();
				Role role = roles.get(0);
				MessageBuilder mb = new MessageBuilder().append(role);
				EmbedBuilder eb = new EmbedBuilder();
				EmbedUtil.setColor(eb);
				eb.setAuthor(sender.getName(), null, sender.getEffectiveAvatarUrl());
				eb.setDescription(args[1]);
				eb.setTimestamp(OffsetDateTime.now());
				mb.setEmbed(eb.build());
				role.getManager().setMentionable(true)
						.queue(s -> channel.sendMessage(mb.build())
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

package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.util.ArrayList;
import java.util.List;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.utils.SimpleLog;

public class Notify implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		final Member member = channel.getGuild().getMember(sender);
		final Guild guild = channel.getGuild();

		if (!guild.equals(Bot.getGuildJda())) {
			channel.sendMessage("You have to be in the JDA GuildCommand to use this feature <" + Bot.INVITE_LINK + ">").queue();
			return;
		}

		if (content.contains("all") || content.contains("both")) {
			final List<Role> roles = new ArrayList<>(3);
			roles.add(Bot.getRoleJdaUpdates());
			roles.add(Bot.getRoleLavaplayerUpdates());
			roles.removeAll(member.getRoles());

			if (roles.size() == 0) {
				guild.getController().removeRolesFromMember(member, Bot.getRoleJdaUpdates(), Bot.getRoleLavaplayerUpdates()).queue(v -> {
					Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + sender.getName() + "#" + sender.getDiscriminator() + " (" + sender.getId() + ") from " + Bot.getRoleJdaUpdates().getName());
					Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + sender.getName() + "#" + sender.getDiscriminator() + " (" + sender.getId() + ") from " + Bot.getRoleLavaplayerUpdates()
							.getName());
				}, Bot.LOG::log);
			} else {
				guild.getController().addRolesToMember(member, roles).queue(v -> roles.forEach(role -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + sender.getName() + "#" + sender
						.getDiscriminator() + " (" + sender.getId() + ") to " + role.getName())), Bot.LOG::log);
			}

		} else {
			final Role role;

			if (content.contains("player")) {
				role = Bot.getRoleLavaplayerUpdates();
			} else {
				role = Bot.getRoleJdaUpdates();
			}

			if (member.getRoles().contains(role)) {
				guild.getController().removeRolesFromMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + sender.getName() + "#" + sender.getDiscriminator() + " ("
						+ sender.getId() + ") from " + role.getName()), Bot.LOG::log);
			} else {
				guild.getController().addRolesToMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + sender.getName() + "#" + sender.getDiscriminator() + " (" + sender
						.getId() + ") to " + role.getName()), Bot.LOG::log);
			}
		}

		message.addReaction("\uD83D\uDC4D").queue();
	}

	@Override
	public String getHelp() {
		return "Notifies you about updates";
	}

	@Override
	public String getName() {
		return "notify";
	}
}

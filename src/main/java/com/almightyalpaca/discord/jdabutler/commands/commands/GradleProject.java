package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.io.IOException;

import com.almightyalpaca.discord.jdabutler.GradleProjectDropboxUploader;
import com.almightyalpaca.discord.jdabutler.commands.Command;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class GradleProject implements Command {
	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		if (!GradleProjectDropboxUploader.GRADLE_PROJECT_ZIP.exists()) {
			channel.sendTyping().queue();
		}
		GradleProjectDropboxUploader.createZip();

		try {
			channel.sendFile(GradleProjectDropboxUploader.GRADLE_PROJECT_ZIP, null).queue();
		} catch (final IOException e) {
			channel.sendMessage("An error occured!").queue();
		}
	}

	@Override
	public String getHelp() {
		return "Prints the download link for an up-to-date gradle example project";
	}

	@Override
	public String getName() {
		return "gradleproject";
	}
}

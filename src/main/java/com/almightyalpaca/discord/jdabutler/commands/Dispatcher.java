package com.almightyalpaca.discord.jdabutler.commands;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.*;
import com.almightyalpaca.discord.jdabutler.commands.commands.Shutdown;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Dispatcher extends ListenerAdapter {

	private final Set<Command>		commands	= ConcurrentHashMap.newKeySet();
	private final ExecutorService	pool		= Executors.newCachedThreadPool();

	{
		this.registerCommand(new HelpCommand());
		this.registerCommand(new VersionsCommand());
		this.registerCommand(new Eval());
		this.registerCommand(new DocsCommand());
		this.registerCommand(new Gradle());
		this.registerCommand(new Maven());
		this.registerCommand(new Jar());
		this.registerCommand(new BuildGradle());
		this.registerCommand(new Notify());
		this.registerCommand(new Ping());
		this.registerCommand(new Uptime());
		this.registerCommand(new Changelog());
		this.registerCommand(new Shutdown());
	}

	public Set<Command> getCommands() {
		return Collections.unmodifiableSet(new HashSet<>(this.commands));
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		final String prefix = Bot.config.getString("prefix");
		final String message = event.getMessage().getRawContent();
		final TextChannel channel = event.getChannel();

		// In DAPI only listen to messages in #java_jda
		if (channel.getGuild().getId().equals("81384788765712384") && !channel.getId().equals("129750718931271681")) {
			return;
		}

		final User sender = event.getAuthor();
		if (message.toLowerCase().startsWith(prefix.toLowerCase())) {
			for (final Command c : this.getCommands()) {
				if (message.toLowerCase().startsWith(prefix.toLowerCase() + c.getName().toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + c.getName().toLowerCase())) {
					this.pool.submit(() -> {
						final String content = this.removePrefix(c.getName(), prefix, event);
						try {
							Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with: " + content);
							c.dispatch(sender, channel, event.getMessage(), content, event);
						} catch (final Exception e) {
							channel.sendMessage(String.format("**There was an error processing your command!**\n```\n%s```", ExceptionUtils.getStackTrace(e))).queue();
						}
					});
					break;
				} else {
					for (final String alias : c.getAliases()) {
						if (message.toLowerCase().startsWith(prefix.toLowerCase() + alias.toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + alias.toLowerCase())) {
							this.pool.submit(() -> {
								try {
									final String content = this.removePrefix(c.getName(), prefix, event);
									Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with: " + content);
									c.dispatch(sender, channel, event.getMessage(), content, event);
								} catch (final Exception e) {
									channel.sendMessage(String.format("**There was an error processing your command!**\n```\n%s```", ExceptionUtils.getStackTrace(e))).queue();
								}
							});
							return;
						}
					}
				}
			}
		}
	}

	public boolean registerCommand(final Command command) {
		if (command.getName().contains(" ")) {
			throw new IllegalArgumentException("Name must not have spaces!");
		}
		if (this.commands.stream().map(Command::getName).anyMatch(c -> command.getName().equalsIgnoreCase(c))) {
			return false;
		}
		this.commands.add(command);
		return true;
	}

	private String removePrefix(final String c, final String prefix, final GuildMessageReceivedEvent event) {
		String content = event.getMessage().getRawContent();
		content = content.substring(c.length() + prefix.length());
		if (content.startsWith(" ")) {
			content = content.substring(1);
		}
		return content;
	}
}

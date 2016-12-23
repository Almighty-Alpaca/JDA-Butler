package com.almightyalpaca.discord.jdabutler.commands.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.eval.Engine;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;

public class EvalCommand implements Command {

	@Override
	public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) {
		if (!Bot.isAdmin(sender)) {
			return;
		}
		final MessageBuilder builder = new MessageBuilder();

		// Execute code
		final Map<String, Object> shortcuts = new HashMap<>();

		shortcuts.put("api", message.getJDA());
		shortcuts.put("jda", message.getJDA());
		shortcuts.put("event", event);

		shortcuts.put("channel", channel);
		shortcuts.put("server", channel.getGuild());
		shortcuts.put("guild", channel.getGuild());

		shortcuts.put("message", message);
		shortcuts.put("msg", message);
		shortcuts.put("me", sender);
		shortcuts.put("bot", message.getJDA().getSelfUser());

		shortcuts.put("config", Bot.config);

		final int timeout = 10;

		final Triple<Object, String, String> result = Engine.GROOVY.eval(shortcuts, Collections.emptyList(), Engine.DEFAULT_IMPORTS, timeout, content);

		if (result.getLeft() instanceof RestAction<?>) {
			((RestAction<?>) result.getLeft()).queue();
		} else if (result.getLeft() != null) {
			builder.appendCodeBlock(result.getLeft().toString(), "");
		}
		if (!result.getMiddle().isEmpty()) {
			builder.append("\n").appendCodeBlock(result.getMiddle(), "");
		}
		if (!result.getRight().isEmpty()) {
			builder.append("\n").appendCodeBlock(result.getRight(), "");
		}

		if (builder.isEmpty()) {
			event.getMessage().addReaction("✅").queue();
		} else {
			for (final Message m : builder.buildAll(SplitPolicy.NEWLINE, SplitPolicy.SPACE, SplitPolicy.ANYWHERE)) {
				event.getChannel().sendMessage(m).queue();
			}
		}
	}

	@Override
	public String getHelp() {
		return null;
	}

	@Override
	public String getName() {
		return "eval";
	}
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.eval.Engine;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Eval implements Command {
    @Override
    public void dispatch(String[] args2, User sender, TextChannel channel, Message message, String content) {
        final String args = Arrays.stream(args2).collect(Collectors.joining(" "));

        if (args.equalsIgnoreCase("list")) {
            final MessageBuilder builder = new MessageBuilder();
            builder.append("List of supported scripting lanugages:", MessageBuilder.Formatting.BOLD).append("\n");
            for (final Engine engine : Engine.values()) {
                builder.append(engine.getName() + "\n");
            }
            message.getChannel().sendMessage(builder.build()).queue();
        } else {
            final MessageBuilder builder = new MessageBuilder();

            // Execute code
            final Map<String, Object> shortcuts = new HashMap<>();

            shortcuts.put("api", message.getJDA());
            shortcuts.put("jda", message.getJDA());
//            shortcuts.put("event", message.getJDA()); // RIP EVENT

            shortcuts.put("channel", channel);
            shortcuts.put("server", channel.getGuild());
            shortcuts.put("guild", channel.getGuild());

            shortcuts.put("message", message);
            shortcuts.put("msg", message);
            shortcuts.put("me", sender);
            shortcuts.put("bot", Bot.jda.getSelfUser());

            shortcuts.put("config", Bot.config);

            final int timeout = 10;

            final Triple<Object, String, String> result = Engine.GROOVY.eval(shortcuts, Collections.emptyList(), Engine.DEFAULT_IMPORTS, timeout, args);

            if (result.getLeft() != null) {
                builder.appendCodeBlock(result.getLeft().toString(), "");
            }
            if (!result.getMiddle().isEmpty()) {
                builder.append("\n").appendCodeBlock(result.getMiddle(), "");
            }
            if (!result.getRight().isEmpty()) {
                builder.append("\n").appendCodeBlock(result.getRight(), "");
            }

            if (builder.length() == 0) {
                builder.append("✅");
            }

            for (final Message m : builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE, MessageBuilder.SplitPolicy.SPACE, MessageBuilder.SplitPolicy.ANYWHERE)) {
                channel.sendMessage(m).queue();
            }
        }
    }

    @Override
    public String getName() {
        return "eval";
    }

    @Override
    public String getHelp() {
        return null;
    }
}

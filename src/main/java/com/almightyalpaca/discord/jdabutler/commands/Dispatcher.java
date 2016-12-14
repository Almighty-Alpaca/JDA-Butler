package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.*;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher extends ListenerAdapter {

    private Set<Command> commands = ConcurrentHashMap.newKeySet();
    private ExecutorService pool = Executors.newCachedThreadPool();

    {
        registerCommand(new HelpCommand());
        registerCommand(new VersionsCommand());
        registerCommand(new Eval());
        registerCommand(new DocsCommand());
        registerCommand(new Gradle());
        registerCommand(new Maven());
        registerCommand(new Jar());
        registerCommand(new BuildGradle());
        registerCommand(new Notify());
        registerCommand(new Ping());
        registerCommand(new Uptime());
        registerCommand(new Changelog());
    }

    public boolean registerCommand(Command command) {
        if (command.getName().contains(" ")) {
            throw new IllegalArgumentException("Name must not have spaces!");
        }
        if (commands.stream().map(Command::getName).anyMatch(c -> command.getName().equalsIgnoreCase(c))) {
            return false;
        }
        commands.add(command);
        return true;
    }

    public Set<Command> getCommands() {
        return Collections.unmodifiableSet(new HashSet<>(commands));
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String prefix = Bot.config.getString("prefix");
        String content = event.getMessage().getRawContent();
        TextChannel channel = event.getChannel();
        if(channel.getGuild().getId().equals("81384788765712384") && !channel.getId().equals("129750718931271681"))
            return;
        User sender = event.getAuthor();
        if (content.toLowerCase().startsWith(prefix.toLowerCase())) {
            for (Command c : getCommands()) {
                if (content.toLowerCase().startsWith(prefix.toLowerCase() + c.getName().toLowerCase() + ' ')
                        || content.equalsIgnoreCase(prefix + c.getName().toLowerCase())) {
                    String[] args = split(content, c.getName(), prefix);
                    pool.submit(() -> {
                        try {
                            Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with split: " + Arrays.toString(args));
                            c.dispatch(args, sender, channel, event.getMessage(), removePrefix(c.getName(), prefix, event));
                        } catch (Exception e) {
                            channel.sendMessage(
                                    String.format("**There was an error processing your command!**\n```\n%s```",
                                            ExceptionUtils.getStackTrace(e))).queue();
                        }
                    });
                    break;
                } else
                    for (String alias : c.getAliases()) {
                        if (content.toLowerCase().startsWith(prefix.toLowerCase() + alias.toLowerCase() + ' ')
                                || content.equalsIgnoreCase(prefix + alias.toLowerCase())) {
                            String[] args = split(content, alias, prefix);
                            pool.submit(() -> {
                                try {
                                    Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with split: " + Arrays.toString(args));
                                    c.dispatch(args, sender, channel, event.getMessage(), removePrefix(alias, prefix, event));
                                } catch (Exception e) {
                                    channel.sendMessage(
                                            String.format("**There was an error processing your command!**\n```\n%s```",
                                                    ExceptionUtils.getStackTrace(e))).queue();
                                }
                            });
                            return;
                        }
                    }
            }
        }
    }

    private String removePrefix(String c, String prefix, GuildMessageReceivedEvent event) {
        String content = event.getMessage().getRawContent();
        content = content.substring(c.length() + prefix.length());
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        return content;
    }

    private String[] split(String content, String c, String prefix) {
        content = content.substring(c.length() + prefix.length());
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        if (content.length() == 0) {
            return new String[0];
        }
        return content.split("\\s");
    }
}

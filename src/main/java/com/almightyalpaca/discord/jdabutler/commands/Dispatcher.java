package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.HelpCommand;
import com.almightyalpaca.discord.jdabutler.commands.commands.VersionsCommand;
import com.almightyalpaca.discord.jdabutler.util.StringUtils;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher extends ListenerAdapter {

    private Map<String, Command> commands = new ConcurrentHashMap<>();
    private ExecutorService pool = Executors.newCachedThreadPool(); // next pr: remove all of these indents

    {
        registerCommand(new HelpCommand());
        registerCommand(new VersionsCommand());
    }

    public boolean registerCommand(Command command) {
        if (command.getName().contains(" ")) {
            throw new IllegalArgumentException("Name must not have spaces!");
        }
        if (commands.containsKey(command.getName().toLowerCase())) {
            return false;
        }
        commands.put(command.getName().toLowerCase(), command);
        return true;
    }

    public Collection<Command> getCommands() {
        return commands.values();
    }


    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String prefix = Bot.config.getString("prefix");
        String content = event.getMessage().getRawContent();
        TextChannel channel = event.getChannel();
        User sender = event.getAuthor();
        if (content.toLowerCase().startsWith(prefix.toLowerCase())) {
            for (Command c : commands.values()) {
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
                                            StringUtils.exceptionToString(e))).queue();
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
                                                    StringUtils.exceptionToString(e))).queue();
                                }
                            });
                            break;
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

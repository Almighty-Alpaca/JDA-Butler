package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.*;
import com.almightyalpaca.discord.jdabutler.commands.commands.moderation.SoftbanCommand;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Dispatcher extends ListenerAdapter
{

    private final Set<Command> commands = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ReactionListenerRegistry reactListReg = new ReactionListenerRegistry();

    public Dispatcher()
    {
        this.registerCommand(new BuildGradleCommand());
        this.registerCommand(new ChangelogCommand());
        this.registerCommand(new DocsCommand(this.reactListReg));
        this.registerCommand(new EvalCommand());
        this.registerCommand(new GradleCommand());
        this.registerCommand(new GradleProjectCommand());
        this.registerCommand(new GuildCommand());
        this.registerCommand(new HelpCommand());
        this.registerCommand(new JarsCommand());
        this.registerCommand(new MavenCommand());
        this.registerCommand(new MavenProjectCommand());
        this.registerCommand(new NotifyCommand());
        this.registerCommand(new PingCommand());
        this.registerCommand(new ShutdownCommand());
        this.registerCommand(new UptimeCommand());
        this.registerCommand(new VersionsCommand());
        this.registerCommand(new AnnouncementCommand());
        this.registerCommand(new SoftbanCommand());
    }

    public Set<Command> getCommands()
    {
        return Collections.unmodifiableSet(new HashSet<>(this.commands));
    }

    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event)
    {
        final String prefix = Bot.config.getString("prefix");
        final String message = event.getMessage().getRawContent();
        final TextChannel channel = event.getChannel();

        // In DAPI only listen to messages in #java_jda
        if (channel.getGuild().getId().equals("81384788765712384") && !channel.getId().equals("129750718931271681"))
            return;

        if (message.toLowerCase().startsWith(prefix.toLowerCase()))
            for (final Command c : this.getCommands())
                if (message.toLowerCase().startsWith(prefix.toLowerCase() + c.getName().toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + c.getName()))
                {
                    this.executeCommand(c, c.getName(), prefix, event);
                    return;
                }
                else
                    for (final String alias : c.getAliases())
                        if (message.toLowerCase().startsWith(prefix.toLowerCase() + alias.toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + alias))
                        {
                            this.executeCommand(c, alias, prefix, event);
                            return;
                        }
    }

    @Override
    public void onMessageReactionAdd(final MessageReactionAddEvent event)
    {
        this.reactListReg.handle(event);
    }

    @Override
    public void onShutdown(final ShutdownEvent event)
    {
        MoreExecutors.shutdownAndAwaitTermination(this.pool, 10, TimeUnit.SECONDS);
    }

    public boolean registerCommand(final Command command)
    {
        if (command.getName().contains(" "))
            throw new IllegalArgumentException("Name must not have spaces!");
        if (this.commands.stream().map(Command::getName).anyMatch(c -> command.getName().equalsIgnoreCase(c)))
            return false;
        this.commands.add(command);
        return true;
    }

    private void executeCommand(final Command c, final String alias, final String prefix, final GuildMessageReceivedEvent event)
    {
        this.pool.submit(() ->
        {
            try
            {
                final String content = this.removePrefix(alias, prefix, event);
                Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with: " + content);
                c.dispatch(event.getAuthor(), event.getChannel(), event.getMessage(), content, event);
            }
            catch (final Exception e)
            {
                event.getChannel().sendMessage("**There was an error processing your command!**").queue();
                Bot.LOG.log(e);
            }
        });
    }

    private String removePrefix(final String c, final String prefix, final GuildMessageReceivedEvent event)
    {
        String content = event.getMessage().getRawContent();
        content = content.substring(c.length() + prefix.length());
        if (content.startsWith(" "))
            content = content.substring(1);
        return content;
    }

    public static class ReactionListenerRegistry
    {
        private final Set<ReactionCommand.ReactionListener> listeners;

        private ReactionListenerRegistry()
        {
            this.listeners = new HashSet<>();
        }

        public void register(final ReactionCommand.ReactionListener listener)
        {
            synchronized (this.listeners)
            {
                this.listeners.add(listener);
            }
        }

        public void remove(final ReactionCommand.ReactionListener listener)
        {
            synchronized (this.listeners)
            {
                this.listeners.remove(listener);
            }
        }

        private void handle(final MessageReactionAddEvent event)
        {
            synchronized (this.listeners)
            {
                for (final ReactionCommand.ReactionListener listener : this.listeners)
                    listener.handle(event);
            }
        }
    }
}

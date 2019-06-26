package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.*;
import com.almightyalpaca.discord.jdabutler.commands.commands.moderation.SoftbanCommand;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.google.common.util.concurrent.MoreExecutors;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dispatcher extends ListenerAdapter
{

    private final Set<Command> commands = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newCachedThreadPool(MiscUtils.newThreadFactory("command-runner", false));
    private final ReactionListenerRegistry reactListReg = new ReactionListenerRegistry();

    public Dispatcher()
    {
        this.registerCommand(new BuildGradleCommand());
        this.registerCommand(new ChangelogCommand());
        this.registerCommand(new DateVersionCommand());
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
        this.registerCommand(new SlowmodeCommand());
        this.registerCommand(new UpdateCommand());
    }

    public Set<Command> getCommands()
    {
        return Collections.unmodifiableSet(new HashSet<>(this.commands));
    }

    @Override
    public void onGuildMessageReceived(final GuildMessageReceivedEvent event)
    {
        if(event.getAuthor().isBot() || event.getAuthor().isFake())
            return;

        final String prefix = Bot.config.getString("prefix");
        String message = event.getMessage().getContentRaw();

        if (Bot.isStealth)
        {
            if (message.startsWith(prefix) && message.startsWith("fake", prefix.length()))
            {
                message = prefix + message.substring(prefix.length() + 4); //change back to !cmd
            }
            else
            {
                return;
            }
        }

        final TextChannel channel = event.getChannel();

        if (channel.getGuild().getIdLong() == 81384788765712384L                                             // if DAPI
            && !(channel.getIdLong() == 381889648827301889L                                                  // and not #java_jda
                || (channel.getParent() != null && channel.getParent().getIdLong() == 356505966201798656L))) // or not testing category
            return;                                                                                          // ignore message

        if (message.toLowerCase().startsWith(prefix.toLowerCase()))
            for (final Command c : this.getCommands())
                if (message.toLowerCase().startsWith(prefix.toLowerCase() + c.getName().toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + c.getName()))
                {
                    this.executeCommand(c, c.getName(), prefix, message, event);
                    return;
                }
                else
                    for (final String alias : c.getAliases())
                        if (message.toLowerCase().startsWith(prefix.toLowerCase() + alias.toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + alias))
                        {
                            this.executeCommand(c, alias, prefix, message, event);
                            return;
                        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event)
    {
        Command.removeResponses(event.getChannel(), event.getMessageIdLong(), reactListReg);
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

    private void executeCommand(final Command c, final String alias, final String prefix, final String message,
                                final GuildMessageReceivedEvent event)
    {
        this.pool.submit(() ->
        {
            try
            {
                final String content = this.removePrefix(alias, prefix, message);
                Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with: " + content);
                c.dispatch(event.getAuthor(), event.getChannel(), event.getMessage(), content, event);
            }
            catch (final Exception e)
            {
                event.getChannel().sendMessage("**There was an error processing your command!**").queue(msg ->
                        Command.linkMessage(event.getMessageIdLong(), msg.getIdLong()));
                Bot.LOG.error("Error processing command {}", c.getName(), e);
            }
        });
    }

    private String removePrefix(final String commandName, final String prefix, String content)
    {
        content = content.substring(commandName.length() + prefix.length());
        if (content.startsWith(" "))
            content = content.substring(1);
        return content;
    }

}

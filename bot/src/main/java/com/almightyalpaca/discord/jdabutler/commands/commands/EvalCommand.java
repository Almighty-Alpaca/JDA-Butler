package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.eval.Engine;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.RestAction;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EvalCommand extends Command
{
    private static final int TIMEOUT_S = 10;

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (!Bot.isAdmin(sender))
        {
            reply(event, sender.getAsMention() + " is not in the sudoers file.  This incident will be reported.");
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

        final Triple<Object, String, String> result = Engine.GROOVY.eval(shortcuts, Collections.emptyList(), Engine.DEFAULT_IMPORTS, TIMEOUT_S, content);

        if (result.getLeft() instanceof RestAction<?>)
            ((RestAction<?>) result.getLeft()).queue();
        else if (result.getLeft() != null)
            builder.appendCodeBlock(result.getLeft().toString(), "");
        if (!result.getMiddle().isEmpty())
            builder.append("\n").appendCodeBlock(result.getMiddle(), "");
        if (!result.getRight().isEmpty())
            builder.append("\n").appendCodeBlock(result.getRight(), "");

        if (builder.isEmpty())
            sendSuccess(event.getMessage());
        else
            for (final Message m : builder.buildAll(SplitPolicy.NEWLINE, SplitPolicy.SPACE, SplitPolicy.ANYWHERE))
                reply(event, m);
    }

    @Override
    public String getHelp()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "eval";
    }
}

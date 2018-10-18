package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class SlowmodeCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (!Bot.isHelper(sender))
        {
            this.sendFailed(message);
            return;
        }

        int seconds;

        String args = content.trim().toLowerCase();

        if (args.isEmpty())
        {
            reply(event, "Missing argument: " + getHelp());
            return;
        }
        else if (args.equals("off") || args.equals("false"))
        {
            seconds = 0;
        }
        else
        {
            try
            {
                seconds = Math.max(0, Math.min(Integer.parseInt(args), 120));
            }
            catch (NumberFormatException ignored)
            {
                reply(event, "Could not parse argument");
                return;
            }
        }

        channel.getManager().setSlowmode(seconds).submit()
                .thenRun(() -> sendFailed(event.getMessage()));
    }

    @Override
    public String getHelp()
    {
        return "`slowmode <seconds | off>`";
    }

    @Override
    public String getName()
    {
        return "slowmode";
    }
}

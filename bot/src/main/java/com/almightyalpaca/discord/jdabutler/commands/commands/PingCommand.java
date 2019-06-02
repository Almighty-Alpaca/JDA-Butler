package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.temporal.ChronoUnit;

public class PingCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        reply(event, "Ping: ...", m -> m.editMessage("Ping: " + message.getTimeCreated().until(m.getTimeCreated(), ChronoUnit.MILLIS) + "ms").queue());
    }

    @Override
    public String getHelp()
    {
        return "Pong";
    }

    @Override
    public String getName()
    {
        return "ping";
    }
}

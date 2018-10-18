package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.fakebutler.FakeButlerListener;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class StatsCommand extends Command
{
    private final FakeButlerListener fakeListener;

    public StatsCommand(FakeButlerListener fakeListener)
    {
        this.fakeListener = fakeListener;
    }

    @Override
    public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event)
    {
        reply(event, fakeListener.getStats(event.getJDA()));
    }

    @Override
    public String getHelp()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "stats";
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.fakebutler.FakeButlerListener;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class StatsCommand implements Command
{
    private final FakeButlerListener fakeListener;

    public StatsCommand(FakeButlerListener fakeListener)
    {
        this.fakeListener = fakeListener;
    }

    @Override
    public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event) throws Exception
    {
        channel.sendMessage(fakeListener.getStats(event.getJDA())).queue();
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

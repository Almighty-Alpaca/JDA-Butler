package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class GuildCommand implements Command
{

    private static final String[] ALIASES = new String[]
    { "server" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        channel.sendMessage(Bot.INVITE_LINK).queue();
    }

    @Override
    public String[] getAliases()
    {
        return GuildCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "shows the invite link for the jda guild";
    }

    @Override
    public String getName()
    {
        return "guild";
    }
}

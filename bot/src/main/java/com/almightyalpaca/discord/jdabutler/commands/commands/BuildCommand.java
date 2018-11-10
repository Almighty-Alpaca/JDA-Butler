package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.BotVersion;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class BuildCommand extends Command
{
    @Override
    public void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event)
    {
        if(!Bot.isAdmin(sender)) {
            sendFailed(message);
            return;
        }

        reply(event, String.format("Current JDA-Butler build used:```\n%s```", BotVersion.FULL_VERSION));
    }

    @Override
    public String getHelp()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "build";
    }
}

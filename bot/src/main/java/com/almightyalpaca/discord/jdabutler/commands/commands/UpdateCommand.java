package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.ButlerItem;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class UpdateCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if(!Bot.isAdmin(sender))
        {
            sendFailed(message);
            return;
        }

        VersionedItem butler = VersionCheckerRegistry.getItem("butler");
        if(butler == null)
        {
            reply(event, "Could not find the versioned item");
            return;
        }
        ButlerItem actualItem = (ButlerItem) butler;
        actualItem.onUpdate(butler, butler.getVersion(), false);
    }

    @Override
    public String getHelp()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "update";
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class VersionsCommand implements Command
{

    private static final String[] ALIASES = new String[]
    { "version", "latest" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder();

        EmbedUtil.setColor(eb);

        eb.setAuthor("Latest versions", null, EmbedUtil.JDA_ICON);

        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

        //TODO: Replace with scheduled task
        VersionChecker.checkVersions();

        for (VersionedItem versionedItem : VersionChecker.getVersionedItems())
        {
            if (versionedItem.getUrl() != null)
            {
                eb.addField(versionedItem.getName(), String.format("[%s](%s)", versionedItem.getVersion(), versionedItem.getUrl()), true);
            }
            else
            {
                eb.addField(versionedItem.getName(), versionedItem.getVersion(), true);
            }
        }

        channel.sendMessage(eb.build()).queue();
    }

    @Override
    public String[] getAliases()
    {
        return VersionsCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Prints versions of all the things that matter :D";
    }

    @Override
    public String getName()
    {
        return "versions";
    }
}

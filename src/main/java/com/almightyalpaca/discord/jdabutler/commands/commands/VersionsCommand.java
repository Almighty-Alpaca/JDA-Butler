package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VersionsCommand implements Command
{

    private static final String[] ALIASES = { "version", "latest" };
    private static final String[] DEFAULT_SET = { "jda", "lavaplayer", "jda-utilities" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder();

        EmbedUtil.setColor(eb);

        List<VersionedItem> items;
        if(content.trim().isEmpty())
        {
            items = Arrays.stream(DEFAULT_SET).map(VersionCheckerRegistry::getItem).collect(Collectors.toList());
        }
        else
        {
            String[] split = content.trim().split("\\s+");
            items = Arrays.stream(split).map(VersionCheckerRegistry::getItem).filter(Objects::nonNull).collect(Collectors.toList());
        }

        if(items.isEmpty())
        {
            eb.setAuthor("Latest version", null, EmbedUtil.JDA_ICON);
            eb.setTitle("No item(s) found for input", null);
        }
        else
        {
            if(items.size() == 1)
                eb.setAuthor("Latest version for "+items.get(0).getName(), null, EmbedUtil.JDA_ICON);
            else
                eb.setAuthor("Latest versions", null, EmbedUtil.JDA_ICON);

            for (VersionedItem versionedItem : items)
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

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class MavenCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder().setAuthor("Maven dependencies", null, EmbedUtil.JDA_ICON);

        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getCustomVersionSupplier() == null)
                .collect(Collectors.toList());

        StringBuilder descBuilder = new StringBuilder("If you don't know maven type `!pom.xml` for a complete maven build file\n\n```xml\n");

        descBuilder.append(MavenUtil.getDependencyBlock(items, null));

        descBuilder.append("\n\n");

        descBuilder.append(MavenUtil.getRepositoryBlock(items, null));

        descBuilder.append("\n```");

        eb.setDescription(descBuilder.toString());

        EmbedUtil.setColor(eb);
        channel.sendMessage(eb.build()).queue();
    }

    @Override
    public String getHelp()
    {
        return "Shows maven dependency information";
    }

    @Override
    public String getName()
    {
        return "maven";
    }
}

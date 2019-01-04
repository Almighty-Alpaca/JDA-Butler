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

public class MavenCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder().setAuthor("Maven dependencies", null, EmbedUtil.JDA_ICON);

        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getGroupId() != null && item.getArtifactId() != null && item.getRepoType() != null)
                .collect(Collectors.toList());

        String desc = "If you don't know maven type `!pom.xml` for a complete maven build file\n\n```xml\n" +
                MavenUtil.getDependencyBlock(items, null) +
                "\n\n" +
                MavenUtil.getRepositoryBlock(items, null) +
                "\n```";
        eb.setDescription(desc);

        EmbedUtil.setColor(eb);
        reply(event, eb.build());
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

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleUtil;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class GradleCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder().setAuthor("Gradle dependencies", null, EmbedUtil.getJDAIconUrl());

        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getGroupId() != null && item.getArtifactId() != null && item.getRepoType() != null)
                .collect(Collectors.toList());

        final boolean pretty = content.contains("pretty");

        String description = "If you don't know gradle type `!build.gradle` for a complete gradle build file\n\n```gradle\n"
                + GradleUtil.getDependencyBlock(items, pretty) + "\n"
                + "\n"
                + GradleUtil.getRepositoryBlock(items) + "\n"
                + "```";

        eb.setDescription(description);

        EmbedUtil.setColor(eb);
        reply(event, eb.build());
    }

    @Override
    public String getHelp()
    {
        return "Shows the gradle `compile ...` line";
    }

    @Override
    public String getName()
    {
        return "gradle";
    }
}

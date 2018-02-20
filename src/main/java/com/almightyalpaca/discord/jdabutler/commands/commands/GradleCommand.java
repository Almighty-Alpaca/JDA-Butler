package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class GradleCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder().setAuthor("Gradle dependencies", null, EmbedUtil.JDA_ICON);

        VersionedItem jdaItem = VersionCheckerRegistry.getItem("jda");
        LinkedList<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getCustomVersionSupplier() == null)
                .collect(Collectors.toCollection(LinkedList::new));
        //force jda to be at first position
        items.remove(jdaItem);
        items.addFirst(jdaItem);

        final boolean pretty = content.contains("pretty");

        String description = "If you don't know gradle type `!build.gradle` for a complete gradle build file\n\n```gradle\n"
                + GradleUtil.getDependencyBlock(items, pretty) + "\n"
                + "\n"
                + GradleUtil.getRepositoryBlock(items) + "\n"
                + "```";

        eb.setDescription(description);

        EmbedUtil.setColor(eb);
        channel.sendMessage(eb.build()).queue();
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

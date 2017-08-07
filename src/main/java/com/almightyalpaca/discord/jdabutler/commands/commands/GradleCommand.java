package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GradleCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final MessageBuilder mb = new MessageBuilder();
        final EmbedBuilder eb = new EmbedBuilder();

        final boolean pretty = content.contains("pretty");

        List<VersionedItem> deps = new ArrayList<>(3);
        deps.add(VersionChecker.getItem("jda"));

        String author = "Gradle dependencies for JDA";
        if (content.contains("player"))
        {
            deps.add(VersionChecker.getItem("lavaplayer"));
            author += " and Lavaplayer";
        }
        if (content.toLowerCase().contains("util"))
        {
            deps.add(VersionChecker.getItem("jda-utilities"));
            author += " and JDA-Utilities";
        }

        eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

        String field = "If you don't know gradle type `!build.gradle` for a complete gradle build file\n\n```gradle\n";

        final Collection<Triple<String, String, String>> dependencies = deps.stream()
                .map(item -> Triple.of(item.getGroupId(), item.getArtifactId(), item.getVersion()))
                .collect(Collectors.toList());
        final Collection<Pair<String, String>> repositories = deps.stream()
                .map(item -> item.getRepoType().getGradleImport())
                .distinct()
                .collect(Collectors.toList());

        field += GradleUtil.getDependencyBlock(dependencies, pretty) + "\n";
        field += "\n";

        field += GradleUtil.getRepositoryBlock(repositories) + "\n";

        field += "```";

        eb.addField("", field, false);

        EmbedUtil.setColor(eb);
        mb.setEmbed(eb.build());
        channel.sendMessage(mb.build()).queue();
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

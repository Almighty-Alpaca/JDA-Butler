package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
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

public class BuildGradleCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final MessageBuilder mb = new MessageBuilder();

        List<VersionedItem> deps = new ArrayList<>(3);

        deps.add(VersionCheckerRegistry.getItem("jda"));
        if(content.contains("player"))
            deps.add(VersionCheckerRegistry.getItem("lavaplayer"));
        if(content.toLowerCase().contains("util"))
            deps.add(VersionCheckerRegistry.getItem("jda-utilities"));

        final boolean pretty = content.contains("pretty");

        final Collection<Triple<String, String, String>> dependencies = deps.stream()
                .map(item -> Triple.of(item.getGroupId(), item.getArtifactId(), item.getVersion()))
                .collect(Collectors.toList());
        final Collection<Pair<String, String>> repositories = deps.stream()
                .map(item -> item.getRepoType().getGradleImport())
                .distinct()
                .collect(Collectors.toList());

        mb.appendCodeBlock(GradleUtil.getBuildFile(GradleUtil.DEFAULT_PLUGINS, "com.example.jda.Bot", "1.0", "1.8", dependencies, repositories, pretty), "gradle");
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getHelp()
    {
        return "Shows an example build.gradle file";
    }

    @Override
    public String getName()
    {
        return "build.gradle";
    }
}

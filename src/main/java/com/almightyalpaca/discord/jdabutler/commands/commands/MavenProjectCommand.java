package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MavenProjectCommand implements Command
{
    private static String POM;

    static
    {
        try
        {
            MavenProjectCommand.POM = new BufferedReader(new InputStreamReader(MavenProjectCommand.class.getResourceAsStream("/maven.pom"))).lines().collect(Collectors.joining("\n"));
        }
        catch (final Exception e)
        {
            Bot.LOG.fatal(e);
            MavenProjectCommand.POM = "Load failed.";
        }
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) throws Exception
    {
        List<VersionedItem> deps = new ArrayList<>(3);
        deps.add(VersionChecker.getItem("jda"));
        if (content.contains("player"))
            deps.add(VersionChecker.getItem("lavaplayer"));
        if (content.toLowerCase().contains("util"))
            deps.add(VersionChecker.getItem("jda-utilities"));

        final StringBuilder builder = new StringBuilder();

        //dependency-string:
        for (VersionedItem dep : deps)
        {
            builder.append(MavenUtil.getDependencyString(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), "        "));
        }
        String dependencyString = builder.toString();

        builder.setLength(0);

        //repo-string
        deps.stream().map(VersionedItem::getRepoType).distinct().forEachOrdered(repoType -> {
            builder.append(MavenUtil.getRepositoryString(repoType.toString(), repoType.toString(), repoType.getRepoBase(), "        "));
        });
        String repoString = builder.toString();

        final String pom = String.format(MavenProjectCommand.POM, repoString, dependencyString);
        channel.sendMessage("Here: " + Bot.hastebin(pom) + ".xml").queue();
    }

    @Override
    public String getHelp()
    {
        return "Example maven project";
    }

    @Override
    public String getName()
    {
        return "pom.xml";
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.Lavaplayer;
import com.almightyalpaca.discord.jdabutler.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

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
        final String pom = String.format(MavenProjectCommand.POM, MavenUtil.getRepositoryString("jcenter", "jcenter", "https://jcenter.bintray.com/", "        "), MavenUtil.getDependencyString("net.dv8tion", "JDA", String.valueOf(Bot.config.getString("jda.version.name")), "        ") + (content.contains("lavaplayer") ? MavenUtil.getDependencyString(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion(), "        ") : ""));
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

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.io.IOException;

public class JarsCommand extends Command
{
    private static final String[] ALIASES = new String[]
    { "jar" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {

        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);
        eb.setAuthor("Latest JDA jars", null, EmbedUtil.getJDAIconUrl());
        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

        try
        {
            JenkinsBuild lastBuild = JenkinsApi.JDA_JENKINS.getLastSuccessfulBuild();
            if(lastBuild == null)
            {
                reply(event, "Could not get Artifact-data from CI!");
                return;
            }

            eb.addField("jar", "[download](" + lastBuild.artifacts.get("JDA").getLink() + ")", true);
            eb.addField("javadoc", "[download](" + lastBuild.artifacts.get("JDA-javadoc").getLink() + ")", true);
            eb.addField("sources", "[download](" + lastBuild.artifacts.get("JDA-sources").getLink() + ")", true);
            eb.addField("withDependencies", "[(normal)](" + lastBuild.artifacts.get("JDA-withDependencies").getLink() + ") " +
                    "[(no-opus)](" + lastBuild.artifacts.get("JDA-withDependencies-no-opus").getLink() + ")", true);

            reply(event, eb.build());
        }
        catch(IOException ex)
        {
            Bot.LOG.warn("Failed fetching latest build from Jenkins for Jars command", ex);
            reply(event, "CI was unreachable!");
        }
    }

    @Override
    public String[] getAliases()
    {
        return JarsCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Displays links to all JAR files";
    }

    @Override
    public String getName()
    {
        return "jars";
    }
}

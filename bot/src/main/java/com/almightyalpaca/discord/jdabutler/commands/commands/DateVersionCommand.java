package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.awt.Color;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateVersionCommand extends Command
{
    private static final String DATE_FORMAT = "yyyy-MM-dd; HH:mm:ss";
    private static final String[] ALIASES = { "date", "published" };
    private static final JenkinsApi JENKINS = JenkinsApi.JDA_JENKINS;
    private static final Calendar CAL = Calendar.getInstance();
    private static final DateFormat DFM = getDateFormat();

    private static DateFormat getDateFormat() {
        DateFormat dfm;
        try
        {
            dfm = new SimpleDateFormat(DATE_FORMAT);
        }
        catch (NullPointerException | IllegalArgumentException ex)
        {
            final String defaultFormat = "dd.MM.yyyy; HH:mm:ss";
            Bot.LOG.warn("Given format for DateVersionCommand was not valid, using: " + defaultFormat);
            dfm = new SimpleDateFormat(defaultFormat);
        }
        return dfm;
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder();

        JenkinsBuild build;
        try
        {
            if (content.trim().isEmpty())
                build = JENKINS.getLastSuccessfulBuild();
            else
            {
                String buildNrStr = content;

                final int underscoreIndex = content.indexOf('_');  // in case somebody provided a full version (e. g. 3.8.3_463)
                if (underscoreIndex != -1)
                    buildNrStr = content.substring(underscoreIndex + 1);

                final int buildNr = Integer.parseInt(buildNrStr.trim());
                build = JENKINS.getBuild(buildNr);
            }
        }
        catch (IOException | NumberFormatException ex)
        {
            String title;
            if (ex instanceof IOException)
                title = "Connection to the Jenkins Server timed out!";
            else if (ex instanceof NumberFormatException)
                title = "Given input was not a valid build number!";
            else
                title = "Unknown Error occured!";

            final MessageEmbed failureEmbed = eb.setAuthor("Error occured!", null, EmbedUtil.getJDAIconUrl())
                    .setTitle(title, null)
                    .setColor(Color.RED)
                    .build();
            reply(event, failureEmbed);
            return;
        }

        // Get time of build
        final long buildTime = build.buildTime.toInstant().toEpochMilli();

        CAL.setTimeInMillis(buildTime);
        final String publishedTime = DFM.format(CAL.getTime());

        // Get correct version (copied from JenkinsChangelogProvider#getChangelogs(String, String))
        final String buildVersion = build.status == JenkinsBuild.Status.SUCCESS
                ? build.artifacts.values().iterator().next().fileNameParts.get(1)
                : build.buildNum + " (failed)";

        // Return Info to User
        EmbedUtil.setColor(eb);

        final MessageEmbed successEmbed = eb.setAuthor("Release Time of Version " + buildVersion, build.getUrl(), EmbedUtil.getJDAIconUrl())
            .setTitle(publishedTime, null)
            .build();

        reply(event, successEmbed);
    }

    @Override
    public String[] getAliases()
    {
        return DateVersionCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Prints the datetime when the given build number or the latest build was published";
    }

    @Override
    public String getName()
    {
        return "date";
    }

}

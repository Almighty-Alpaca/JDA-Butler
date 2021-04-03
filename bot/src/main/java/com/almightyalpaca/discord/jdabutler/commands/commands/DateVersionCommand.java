package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.DateUtils;
import com.almightyalpaca.discord.jdabutler.util.DurationUtils;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateVersionCommand extends Command
{
    private static final String[] ALIASES = { "published" };
    private static final JenkinsApi JENKINS = DateUtils.JENKINS;
    private static final DateTimeFormatter FORMATTER = DateUtils.getDateTimeFormatter();

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
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
            Bot.LOG.error("Exception in DateVersionCommand occured!", ex);

            String title;
            if (ex instanceof IOException)
                title = "Connection to the Jenkins Server timed out!";
            else if (ex instanceof NumberFormatException)
                title = "Given input was not a valid build number!";
            else
                title = "Unknown Error occured!";

            final MessageEmbed failureEmbed = new EmbedBuilder().setAuthor("Error occured!", null, EmbedUtil.getJDAIconUrl())
                .setTitle(title, null)
                .setColor(Color.RED)
                .build();
            reply(event, failureEmbed);
            return;
        }

        // Get time of build
        final OffsetDateTime buildTime = build.buildTime;
        final String publishedTime = FORMATTER.format(buildTime);

        final Duration dur = DurationUtils.toDuration(System.currentTimeMillis() - buildTime.toInstant().toEpochMilli());
        final String difference = DurationUtils.formatDuration(dur, true);

        final int lastSpace = difference.lastIndexOf(' ');
        final String differenceWithoutMs = lastSpace < 0 ? difference : difference.substring(0, lastSpace);

        // Get correct version (copied from JenkinsChangelogProvider#getChangelogs(String, String))
        final String buildVersion = build.status == JenkinsBuild.Status.SUCCESS
                ? build.artifacts.values().iterator().next().fileNameParts.get(1)
                : build.buildNum + " (failed)";

        // Return Info to User
        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);

        final MessageEmbed successEmbed = eb.setAuthor("Release Time of Version " + buildVersion, build.getUrl(), EmbedUtil.getJDAIconUrl())
            .setTitle(publishedTime).setDescription(String.format("That was approximately %s ago.", differenceWithoutMs))
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

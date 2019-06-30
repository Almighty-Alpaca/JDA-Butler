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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DateVersionCommand extends Command
{
    private static final String DATE_FORMAT = "yyyy-MM-dd; HH:mm:ss";
    private static final String[] ALIASES = { "published" };
    private static final JenkinsApi JENKINS = JenkinsApi.JDA_JENKINS;
    private static final DateTimeFormatter FORMATTER = getDateTimeFormatter();

    private static DateTimeFormatter getDateTimeFormatter() {
        DateTimeFormatter formatter;
        try
        {
            formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        }
        catch (NullPointerException | IllegalArgumentException ex)
        {
            final String defaultFormat = "dd.MM.yyyy; HH:mm:ss";
            Bot.LOG.warn("Given format for DateVersionCommand was not valid, using: " + defaultFormat);
            formatter = DateTimeFormatter.ofPattern(defaultFormat);
        }
        return formatter;
    }

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
            final String fullStackTrace = getFullStackTrace(ex);
            Bot.LOG.error("Exception in DateVersionCommand occured!" + System.lineSeparator() + fullStackTrace);

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
        OffsetDateTime buildTime = build.buildTime;
        final String publishedTime = FORMATTER.format(buildTime);

        // Get correct version (copied from JenkinsChangelogProvider#getChangelogs(String, String))
        final String buildVersion = build.status == JenkinsBuild.Status.SUCCESS
                ? build.artifacts.values().iterator().next().fileNameParts.get(1)
                : build.buildNum + " (failed)";

        // Return Info to User
        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);

        final MessageEmbed successEmbed = eb.setAuthor("Release Time of Version " + buildVersion, build.getUrl(), EmbedUtil.getJDAIconUrl())
            .setTitle(publishedTime, null)
            .build();

        reply(event, successEmbed);
    }

    private String getFullStackTrace(Exception ex) {
        List<StackTraceElement> stackTraceElements = Arrays.asList(ex.getStackTrace());
        String stacktrace = stackTraceElements.stream().map(entry -> "\tat " + entry.toString()).collect(Collectors.joining(System.lineSeparator()));

        StringBuilder builder = new StringBuilder(String.format("%s: %s" + System.lineSeparator(), ex.getClass().getName(), ex.getMessage()));
        builder.append(stacktrace);
        return builder.toString();
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

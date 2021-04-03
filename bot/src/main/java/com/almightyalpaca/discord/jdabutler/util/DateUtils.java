package com.almightyalpaca.discord.jdabutler.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;

public final class DateUtils
{
    private static final String DATE_FORMAT = "dd/MM/yyyy 'at' HH:mm:ss";
    public static final JenkinsApi JENKINS = JenkinsApi.JDA_JENKINS;
    public static final DateTimeFormatter FORMATTER = getDateTimeFormatter();

    public static DateTimeFormatter getDateTimeFormatter() {
        DateTimeFormatter formatter;
        try
        {
            formatter = DateTimeFormatter.ofPattern(DATE_FORMAT + " (z)");
        }
        catch (NullPointerException | IllegalArgumentException ex)
        {
            final String defaultFormat = "dd.MM.yyyy 'at' HH:mm:ss (z)";
            Bot.LOG.warn("Given format for DateVersionCommand was not valid, using: " + defaultFormat);
            formatter = DateTimeFormatter.ofPattern(defaultFormat);
        }

        return formatter.withZone(ZoneId.of("UTC"));
    }

    // prevent instantiation
    private DateUtils() {}

}

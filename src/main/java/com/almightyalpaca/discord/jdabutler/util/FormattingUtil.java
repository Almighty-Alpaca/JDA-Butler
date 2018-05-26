package com.almightyalpaca.discord.jdabutler.util;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsUser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;

import java.text.DateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FormattingUtil
{

    public static String formatTimestap(final long timestap)
    {
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.ENGLISH).format(Date.from(Instant.ofEpochMilli(timestap)));
    }

    public static void setFooter(final EmbedBuilder eb, final List<JenkinsUser> culprits, OffsetDateTime timestamp)
    {
        eb.setTimestamp(timestamp);
        if (culprits.size() == 1)
        {
            JenkinsUser author = culprits.get(0);
            final String description = author.description;

            if (description != null)
            {
                User user = null;
                String friendlyName = null;

                for (final String line : description.split("\r?\n"))
                {
                    if (line.startsWith("discord:"))
                    {
                        try
                        {
                            user = Bot.jda.getUserById(line.substring(8).trim());
                            break;
                        }
                        catch(NumberFormatException ignored) {}
                    }
                    else if (line.startsWith("name:"))
                    {
                        friendlyName = line.substring(5).trim();
                    }
                }

                if (user != null)
                    eb.setFooter(user.getName(), user.getAvatarUrl());
                else if (friendlyName != null)
                    eb.setFooter(friendlyName, null);
                else
                    eb.setFooter(author.toString(), null);
            }
            else
                eb.setFooter(author.toString(), null);
        }
        else if (culprits.size() > 1)
            eb.setFooter("Multiple users", null);
        else
            eb.setFooter("Unknown user", null);
    }
}

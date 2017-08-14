package com.almightyalpaca.discord.jdabutler;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsChange;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsUser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.User;

import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FormattingUtil
{

    public static String formatTimestap(final long timestap)
    {
        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.ENGLISH).format(Date.from(Instant.ofEpochMilli(timestap)));
    }

    public static List<String> getChangelog(List<JenkinsChange> changeSet)
    {
        final List<String> fields = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < changeSet.size(); i++)
        {
            final JenkinsChange item = changeSet.get(i);

            final String[] lines = item.commitMsg.split("\n");

            for (int j = 0; j < lines.length; j++)
            {
                final StringBuilder line = new StringBuilder();
                line.append("[`").append(j == 0 ? item.getShortId() : "`.......`").append("`](https://github.com/DV8FromTheWorld/JDA/commit/" + item.commitId + ")").append(" ").append(lines[j]).append("\n");

                if (builder.length() + line.length() > 1021)
                {
                    fields.add(builder.toString());
                    builder = new StringBuilder();
                }

                builder.append(line);
            }
        }

        if (builder.length() > 0)
            fields.add(builder.toString());

        return fields;

    }

    public static void setFooter(final EmbedBuilder eb, final List<JenkinsUser> culprits, final String timestamp)
    {

        if (culprits.size() == 1)
        {
            JenkinsUser author = culprits.get(0);
            final String description = author.description;

            if (description != null)
            {
                User user = null;

                for (final String line : description.split("\n"))
                    if (line.startsWith("discord: "))
                    {
                        user = Bot.jda.getUserById(line.substring(7));
                        break;
                    }

                if (user != null)
                    eb.setFooter(user.getName() + "   |    " + timestamp, user.getAvatarUrl());
                else
                    eb.setFooter(author.toString() + "   |   " + timestamp, null);
            }
            else
                eb.setFooter(author.toString() + "   |   " + timestamp, null);
        }
        else if (culprits.size() > 1)
            eb.setFooter("multiple users   |   " + timestamp, null);
        else
            eb.setFooter(timestamp, null);

    }
}

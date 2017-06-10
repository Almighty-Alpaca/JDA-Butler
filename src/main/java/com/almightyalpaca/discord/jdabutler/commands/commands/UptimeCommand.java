package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.StringUtils;
import java.lang.management.ManagementFactory;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class UptimeCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final long duration = ManagementFactory.getRuntimeMXBean().getUptime();

        final long years = duration / 31104000000L;
        final long months = duration / 2592000000L % 12;
        final long days = duration / 86400000L % 30;
        final long hours = duration / 3600000L % 24;
        final long minutes = duration / 60000L % 60;
        final long seconds = duration / 1000L % 60;

        String uptime = "";
        uptime += years == 0 ? "" : years + " Year" + (years > 1 ? "s" : "") + ", ";
        uptime += months == 0 ? "" : months + " Month" + (months > 1 ? "s" : "") + ", ";
        uptime += days == 0 ? "" : days + " Day" + (days > 1 ? "s" : "") + ", ";
        uptime += hours == 0 ? "" : hours + " Hour" + (hours > 1 ? "s" : "") + ", ";
        uptime += minutes == 0 ? "" : minutes + " Minute" + (minutes > 1 ? "s" : "") + ", ";
        uptime += seconds == 0 ? "" : seconds + " Second" + (seconds > 1 ? "s" : "") + ", ";

        uptime = StringUtils.replaceLast(uptime, ", ", "");
        uptime = StringUtils.replaceLast(uptime, ",", " and");

        channel.sendMessage(uptime).queue();
    }

    @Override
    public String getHelp()
    {
        return "Shows my uptime";
    }

    @Override
    public String getName()
    {
        return "uptime";
    }
}

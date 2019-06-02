package com.almightyalpaca.discord.jdabutler.util;

import com.almightyalpaca.discord.jdabutler.Bot;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

public class EmbedUtil
{

    public static final Color COLOR_JDA_PURPLE = Color.decode("#9158BC");

    public static String getJDAIconUrl()
    {
        return Bot.getGuildJda().getIconUrl();
    }

    public static void setColor(final EmbedBuilder builder)
    {
        builder.setColor(EmbedUtil.COLOR_JDA_PURPLE);
    }

}

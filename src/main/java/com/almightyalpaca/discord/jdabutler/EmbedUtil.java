package com.almightyalpaca.discord.jdabutler;

import java.awt.Color;

import net.dv8tion.jda.core.EmbedBuilder;

public class EmbedUtil {

	public static final Color COLOR_JDA_PRUPLE = Color.decode("#9158BC");

	public static void setColor(final EmbedBuilder builder) {
		builder.setColor(EmbedUtil.COLOR_JDA_PRUPLE);
	}
}

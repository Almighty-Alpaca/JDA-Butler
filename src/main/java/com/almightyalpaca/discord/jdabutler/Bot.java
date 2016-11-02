package com.almightyalpaca.discord.jdabutler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kantenkugel.discordbot.moduleutils.DocParser;

import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import net.dv8tion.jda.core.utils.SimpleLog.Level;
import net.dv8tion.jda.core.utils.SimpleLog.LogListener;

public class Bot {

	public static JDAImpl					jda;
	public static Config					config;
	public static EventListener				listener;

	public static TextChannel				CHANNEL_ANNOUNCEMENTS;
	public static TextChannel				CHANNEL_LOGS;
	public static Guild						GUILD_JDA;
	public static Role						ROLE_JDA_PLAYER_UPDATES;
	public static Role						ROLE_JDA_3_UPDATES;
	public static Role						ROLE_JDA_UPDATES;
	public static Role						ROLE_BOTS;
	public static Role						ROLE_JDA_FANCLUB;
	public static Role						ROLE_STAFF;

	public static final SimpleLog			LOG			= SimpleLog.getLog("Bot");

	private static final SimpleDateFormat	DATEFORMAT	= new SimpleDateFormat("HH:mm:ss");
	private static final String				LOGFORMAT	= "[%time%] [%level%] [%name%]: %text%";

	public static void main(final String[] args) throws JsonIOException, JsonSyntaxException, WrongTypeException, KeyNotFoundException, IOException, LoginException, IllegalArgumentException,
			InterruptedException, RateLimitedException, NoSuchFieldException, SecurityException, IllegalAccessException {

		DocParser.init();

		SimpleLog.addFileLogs(new File("out.log"), new File("err.log"));

		Bot.config = ConfigFactory.getConfig(new File("config.json"));

		final JDABuilder builder = new JDABuilder(AccountType.BOT);
		builder.setBulkDeleteSplittingEnabled(false);

		final String token = Bot.config.getString("discord.token", "Your token");
		builder.setToken(token);

		final String proxyAdress = Bot.config.getString("proxy.host", "");
		final int proxyPort = Bot.config.getInt("proxy.port", 8080);
		final boolean useProxy = Bot.config.getBoolean("proxy.use", false);
		if (useProxy) {
			builder.setProxy(new HttpHost(proxyAdress, proxyPort));
		}

		Bot.config.save();
		Bot.listener = new EventListener();
		builder.addListener(Bot.listener);

		Bot.jda = (JDAImpl) builder.buildBlocking();

		Bot.CHANNEL_ANNOUNCEMENTS = Bot.jda.getTextChannelById("125227483518861312");
		Bot.CHANNEL_LOGS = Bot.jda.getTextChannelById("241926199666802690");

		Bot.GUILD_JDA = Bot.jda.getGuildById("125227483518861312");

		Bot.ROLE_JDA_PLAYER_UPDATES = Bot.GUILD_JDA.getRoleById("241948768113524762");
		Bot.ROLE_JDA_3_UPDATES = Bot.GUILD_JDA.getRoleById("241948856978374657");
		Bot.ROLE_JDA_UPDATES = Bot.GUILD_JDA.getRoleById("241948671325765632");

		Bot.ROLE_BOTS = Bot.GUILD_JDA.getRoleById("125616720156033024");
		Bot.ROLE_JDA_FANCLUB = Bot.GUILD_JDA.getRoleById("169558668126322689");
		Bot.ROLE_STAFF = Bot.GUILD_JDA.getRoleById("169481978268090369");

		SimpleLog.addListener(new LogListener() {

			@Override
			public void onError(final SimpleLog log, final Throwable t) {
				log.log(Level.FATAL, ExceptionUtils.getStackTrace(t));
			}

			@Override
			public void onLog(final SimpleLog log, final Level level, final Object message) {
				if (level.isError()) {
					final String format = Bot.LOGFORMAT.replace("%time%", Bot.DATEFORMAT.format(new Date())).replace("%level%", level.getTag()).replace("%name%", log.name).replace("%text%", String
							.valueOf(message));
					Bot.CHANNEL_LOGS.sendMessage(format).queue();
				}
			}
		});

		EventListener.start();

		// Bot.jda.getAccountManager().setGame("JDA");
		// Bot.jda.getAccountManager().update();

		// This didn't worked
		// final Field clientField = JDAImpl.class.getDeclaredField("client");
		// clientField.setAccessible(true);
		// final WebSocketClient client = (WebSocketClient) clientField.get(Bot.jda);
		//
		// final Field socketField = WebSocketClient.class.getDeclaredField("socket");
		// socketField.setAccessible(true);
		// final WebSocket socket = (WebSocket) socketField.get(client);
		//
		// socket.getSocket().setSoTimeout(1000 * 10);

	}

	public static void shutdown() {
		Bot.jda.shutdown();
		EventListener.executor.shutdown();
		System.exit(0);
	}
}

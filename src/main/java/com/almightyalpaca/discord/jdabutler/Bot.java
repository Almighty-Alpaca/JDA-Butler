package com.almightyalpaca.discord.jdabutler;

import com.almightyalpaca.discord.jdabutler.commands.Dispatcher;
import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kantenkugel.discordbot.moduleutils.DocParser;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.SplitPolicy;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import net.dv8tion.jda.core.utils.SimpleLog.Level;
import net.dv8tion.jda.core.utils.SimpleLog.LogListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Bot {

	public static JDAImpl					jda;
	public static Config					config;
	public static EventListener				listener;
	public static Dispatcher				dispatcher;

	public static final SimpleLog			LOG			= SimpleLog.getLog("Bot");

	private static final SimpleDateFormat	DATEFORMAT	= new SimpleDateFormat("HH:mm:ss");

	private static final String				LOGFORMAT	= "[%time%] [%level%] [%name%]: %text%";
	public static final String				INVITE_LINK	= "https://discord.gg/0hMr4ce0tIk3pSjp";

	public static TextChannel getChannelAnnouncements() {
		return Bot.jda.getTextChannelById("125227483518861312");
	}

	public static TextChannel getChannelLogs() {
		return Bot.jda.getTextChannelById("241926199666802690");
	}

	public static TextChannel getChannelTesting() {
		return Bot.jda.getTextChannelById("115567590495092740");
	}

	public static Guild getGuildJda() {
		return Bot.jda.getGuildById("125227483518861312");
	}

	public static Role getRoleBots() {
		return Bot.getGuildJda().getRoleById("125616720156033024");
	}

	public static Role getRoleJdaFanclub() {
		return Bot.getGuildJda().getRoleById("169558668126322689");
	}

	public static Role getRoleJdaUpdates() {
		return Bot.getGuildJda().getRoleById("241948671325765632");
	}

	public static Role getRoleLavaplayerUpdates() {
		return Bot.getGuildJda().getRoleById("241948768113524762");
	}

	public static Role getRoleStaff() {
		return Bot.getGuildJda().getRoleById("169481978268090369");
	}

	public static Role getRoleHelper() {
		return getGuildJda().getRoleById("183963327033114624");
	}

	public static boolean isAdmin(final User user) {
		final Member member = Bot.getGuildJda().getMember(user);
		return member != null && member.getRoles().contains(Bot.getRoleStaff());
	}

	public static void main(final String[] args) throws JsonIOException, JsonSyntaxException, WrongTypeException, KeyNotFoundException, IOException, LoginException, IllegalArgumentException,
			InterruptedException, RateLimitedException, NoSuchFieldException, SecurityException, IllegalAccessException {

		EventListener.executor.submit(DocParser::init);

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
		builder.addListener(Bot.dispatcher = new Dispatcher());

		Bot.jda = (JDAImpl) builder.buildBlocking();

		SimpleLog.addListener(new LogListener() {

			@Override
			public void onError(final SimpleLog log, final Throwable t) {
				log.log(Level.FATAL, ExceptionUtils.getStackTrace(t));
			}

			@Override
			public void onLog(final SimpleLog log, final Level level, final Object message) {
				try {
					if (level.getPriority() >= Level.INFO.getPriority()) {
						String format = "`" + Bot.LOGFORMAT.replace("%time%", Bot.DATEFORMAT.format(new Date())).replace("%level%", level.getTag()).replace("%name%", log.name).replace("%text%", String
								.valueOf(message)) + "`";
						if (format.length() >= 2000) {
							format = format.substring(0, 1999);
						}
						final TextChannel channel = Bot.getChannelLogs();
						if (channel != null) {
							for (final Message m : new MessageBuilder().append(format).buildAll(SplitPolicy.NEWLINE, SplitPolicy.SPACE, SplitPolicy.ANYWHERE)) {
								channel.sendMessage(m).queue();
							}
						}
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});

		EventListener.start();

		Bot.jda.getPresence().setGame(Game.of("JDA"));

	}

	public static void shutdown() {
		Bot.jda.shutdown();
	}

	public static boolean isHelper(User sender) {
		return getGuildJda().isMember(sender) &&
				(isAdmin(sender) || getGuildJda().getMember(sender).getRoles().contains(getRoleHelper()));
	}
}

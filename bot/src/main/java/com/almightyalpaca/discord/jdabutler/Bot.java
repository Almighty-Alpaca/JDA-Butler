package com.almightyalpaca.discord.jdabutler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import com.almightyalpaca.discord.jdabutler.commands.Dispatcher;
import com.almightyalpaca.discord.jdabutler.commands.commands.NotifyCommand;
import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleProjectDropboxUtil;
import com.almightyalpaca.discord.jdabutler.util.logging.WebhookAppender;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kantenkugel.discordbot.fakebutler.FakeButlerListener;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.JDAImpl;
import okhttp3.OkHttpClient;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot
{

    public static Config config;
    public static Dispatcher dispatcher;
    public static final String INVITE_LINK = "https://discord.gg/0hMr4ce0tIk3pSjp";
    public static JDAImpl jda;
    public static boolean isStealth = false;

    public static OkHttpClient httpClient;

    public static EventListener listener;

    public static final Logger LOG = (Logger) LoggerFactory.getLogger(Bot.class);

    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(MiscUtils.newThreadFactory("main-executor"));

    public static Guild getGuildJda()
    {
        return Bot.jda.getGuildById("125227483518861312");
    }

    public static Role getRoleBots()
    {
        return Bot.getGuildJda().getRoleById("125616720156033024");
    }

    public static Role getRoleStaff()
    {
        return Bot.getGuildJda().getRoleById("169481978268090369");
    }

    public static boolean isAdmin(final User user)
    {
        final Member member = Bot.getGuildJda().getMember(user);
        return member != null && member.getRoles().contains(Bot.getRoleStaff());
    }

    public static Role getRoleHelper()
    {
        return Bot.getGuildJda().getRoleById("183963327033114624");
    }

    public static boolean isHelper(final User user)
    {
        if(isAdmin(user))
            return true;
        final Member member = Bot.getGuildJda().getMember(user);
        return member != null && member.getRoles().contains(Bot.getRoleHelper());
    }

    public static void main(final String[] args) throws JsonIOException, JsonSyntaxException, WrongTypeException, KeyNotFoundException, IOException, LoginException, IllegalArgumentException, InterruptedException, SecurityException
    {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                LOG.error("There was an uncaught exception in thread {}", thread.getName(), throwable));
        Bot.httpClient = new OkHttpClient();

        EXECUTOR.submit(JDoc::init);

        Bot.config = ConfigFactory.getConfig(new File("config.json"));

        final JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setBulkDeleteSplittingEnabled(false);

        final String token = Bot.config.getString("discord.token", "Your token");
        builder.setToken(token);

        Bot.config.save();
        Bot.listener = new EventListener();
        builder.addEventListeners(Bot.listener);
        builder.addEventListeners(Bot.dispatcher = new Dispatcher());
        builder.addEventListeners(new FakeButlerListener());

        builder.setActivity(Activity.playing("JDA"));

        Bot.jda = (JDAImpl) builder.build().awaitReady();

        if(Bot.config.getBoolean("webhook.enabled", false)) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            ThresholdFilter filter = new ThresholdFilter();
            filter.setLevel(Bot.config.getString("webhook.level"));
            filter.setContext(lc);
            filter.start();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setPattern(Bot.config.getString("webhook.pattern"));
            encoder.setContext(lc);
            encoder.start();

            WebhookAppender appender = new WebhookAppender();
            appender.setEncoder(encoder);
            appender.addFilter(filter);
            appender.setWebhookUrl(Bot.config.getString("webhook.webhookurl"));
            appender.setName("ERROR_WH");
            appender.setContext(lc);
            appender.start();

            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.addAppender(appender);
        }

        NotifyCommand.reloadBlacklist(null);

        EXECUTOR.submit(() ->
        {
            VersionCheckerRegistry.init();
            VersionedItem jdaItem = VersionCheckerRegistry.getItem("jda");
            if(jdaItem.getVersion() != null && jdaItem.parseVersion().build != config.getInt("jda.version.build"))
            {
                //do not announce here as that might cause duplicate announcements when a new instance is fired up (or a very old one)
                jdaItem.getUpdateHandler().onUpdate(jdaItem, config.getString("jda.version.name"), false);
            }
            else
            {
                GradleProjectDropboxUtil.fetchUrl();
            }
        });
    }

    public static void shutdown()
    {
        shutdown(0);
    }

    public static void shutdown(int code)
    {
        Bot.jda.removeEventListener(Bot.jda.getRegisteredListeners());

        try
        {
            TimeUnit.SECONDS.sleep(2);
        }
        catch (final InterruptedException ignored)
        {}

        Bot.jda.shutdownNow();
        System.exit(code);
    }
}

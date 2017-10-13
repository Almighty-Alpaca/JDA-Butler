package com.almightyalpaca.discord.jdabutler;

import com.almightyalpaca.discord.jdabutler.commands.Dispatcher;
import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import okhttp3.OkHttpClient;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Bot
{

    public static Config config;
    public static Dispatcher dispatcher;
    public static final String INVITE_LINK = "https://discord.gg/0hMr4ce0tIk3pSjp";
    public static JDAImpl jda;

    public static OkHttpClient httpClient;

    public static EventListener listener;

    public static final SimpleLog LOG = SimpleLog.getLog("Bot");

//    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("HH:mm:ss");
//    private static final String LOGFORMAT = "[%time%] [%level%] [%name%]: %text%";

    public static TextChannel getChannelAnnouncements()
    {
        return Bot.jda.getTextChannelById("125227483518861312");
    }

    public static TextChannel getChannelExperimental()
    {
        return Bot.jda.getTextChannelById("289742061220134912");
    }

    public static TextChannel getChannelLavaplayer()
    {
        return Bot.jda.getTextChannelById("263484072389640193");
    }

    public static TextChannel getChannelLogs()
    {
        return Bot.jda.getTextChannelById("241926199666802690");
    }

    public static TextChannel getChannelTesting()
    {
        return Bot.jda.getTextChannelById("115567590495092740");
    }

    public static Guild getGuildJda()
    {
        return Bot.jda.getGuildById("125227483518861312");
    }

    public static Role getRoleBots()
    {
        return Bot.getGuildJda().getRoleById("125616720156033024");
    }

    public static Role getRoleExperimentalUpdates()
    {
        return Bot.getGuildJda().getRoleById("289744006433472513");
    }

    public static Role getRoleHelper()
    {
        return Bot.getGuildJda().getRoleById("183963327033114624");
    }

    public static Role getRoleJdaFanclub()
    {
        return Bot.getGuildJda().getRoleById("169558668126322689");
    }

    public static Role getRoleJdaUpdates()
    {
        return Bot.getGuildJda().getRoleById("241948671325765632");
    }

    public static Role getRoleLavaplayerUpdates()
    {
        return Bot.getGuildJda().getRoleById("241948768113524762");
    }

    public static Role getRoleStaff()
    {
        return Bot.getGuildJda().getRoleById("169481978268090369");
    }

    public static String hastebin(final String text)
    {
        try
        {
            return "https://hastebin.com/" + Unirest.post("https://hastebin.com/documents").header("User-Agent", "Mozilla/5.0 JDA-Butler").header("Content-Type", "text/plain").body(text).asJson().getBody().getObject().getString("key");
        }
        catch (final UnirestException e)
        {
            Bot.LOG.fatal(e);
            return null;
        }
    }

    public static boolean isAdmin(final User user)
    {
        final Member member = Bot.getGuildJda().getMember(user);
        return member != null && member.getRoles().contains(Bot.getRoleStaff());
    }

    public static boolean isHelper(final User sender)
    {
        return Bot.getGuildJda().isMember(sender) && (Bot.isAdmin(sender) || Bot.getGuildJda().getMember(sender).getRoles().contains(Bot.getRoleHelper()));
    }

    public static void main(final String[] args) throws JsonIOException, JsonSyntaxException, WrongTypeException, KeyNotFoundException, IOException, LoginException, IllegalArgumentException, InterruptedException, RateLimitedException, NoSuchFieldException, SecurityException, IllegalAccessException
    {
        Bot.httpClient = new OkHttpClient.Builder().build();

        EventListener.executor.submit(JDoc::init);

        Bot.config = ConfigFactory.getConfig(new File("config.json"));

        final JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setAudioEnabled(false);
        builder.setBulkDeleteSplittingEnabled(false);

        final String token = Bot.config.getString("discord.token", "Your token");
        builder.setToken(token);

        Bot.config.save();
        Bot.listener = new EventListener();
        builder.addEventListener(Bot.listener);
        builder.addEventListener(Bot.dispatcher = new Dispatcher());

        builder.setGame(Game.of("JDA"));

        Bot.jda = (JDAImpl) builder.buildBlocking();

//        SimpleLog.addListener(new LogListener()
//        {
//
//            @Override
//            public void onError(final SimpleLog log, final Throwable t)
//            {
//                log.log(Level.FATAL, ExceptionUtils.getStackTrace(t));
//            }
//
//            @Override
//            public void onLog(final SimpleLog log, final Level level, final Object message)
//            {
//                try
//                {
//                    if (level.getPriority() >= Level.INFO.getPriority())
//                    {
//                        String format = "`" + Bot.LOGFORMAT.replace("%time%", Bot.DATEFORMAT.format(new Date())).replace("%level%", level.getTag()).replace("%name%", log.name).replace("%text%", String.valueOf(message)) + "`";
//                        if (format.length() >= 2000)
//                            format = format.substring(0, 1999);
//                        final TextChannel channel = Bot.getChannelLogs();
//                        if (channel != null)
//                            for (final Message m : new MessageBuilder().append(format).buildAll(SplitPolicy.NEWLINE, SplitPolicy.SPACE, SplitPolicy.ANYWHERE))
//                                channel.sendMessage(m).queue();
//                    }
//                }
//                catch (final Exception e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        });

        EventListener.start();
    }

    public static void shutdown()
    {
        Bot.jda.removeEventListener(Bot.jda.getRegisteredListeners());

        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (final InterruptedException ignored)
        {}

        Bot.jda.shutdown();
    }
}
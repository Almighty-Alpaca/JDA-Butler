package com.almightyalpaca.discord.jdabutler.util;

import com.almightyalpaca.discord.jdabutler.Bot;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MiscUtils
{
    private static String HASTEBIN_SERVER = "https://hastebin.de/"; //requires trailing slash

    public static ThreadFactory newThreadFactory(String threadName)
    {
        return newThreadFactory(threadName, Bot.LOG);
    }

    public static ThreadFactory newThreadFactory(String threadName, boolean isDaemon)
    {
        return newThreadFactory(threadName, Bot.LOG, isDaemon);
    }

    public static ThreadFactory newThreadFactory(String threadName, Logger logger)
    {
        return newThreadFactory(threadName, logger, true);
    }

    public static ThreadFactory newThreadFactory(String threadName, Logger logger, boolean isdaemon)
    {
        return (r) ->
        {
            Thread t = new Thread(r, threadName);
            t.setDaemon(isdaemon);
            t.setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) ->
                    logger.error("There was a uncaught exception in the {} threadpool", thread.getName(), throwable));
            return t;
        };
    }

    public static String hastebin(final String text)
    {
        try(Response response = Bot.httpClient.newCall(
                new Request.Builder()
                        .post(RequestBody.create(MediaType.parse("text/plain"), text))
                        .url(HASTEBIN_SERVER + "documents")
                        .header("User-Agent", "Mozilla/5.0 JDA-Butler")
                        .build()
        ).execute())
        {
            if(!response.isSuccessful())
                return null;

            JSONObject obj = new JSONObject(new JSONTokener(response.body().charStream()));
            return HASTEBIN_SERVER + obj.getString("key");
        }

        catch (final Exception e)
        {
            Bot.LOG.warn("Error posting text to hastebin", e);
            return null;
        }
    }

    public static void announce(TextChannel channel, Role role, Message message, boolean slowmode)
    {
        if (slowmode)
        {
            channel.getManager().setSlowmode(30).queue(v -> channel.getManager().setSlowmode(0).queueAfter(2, TimeUnit.MINUTES));
        }

        role.getManager().setMentionable(true)                  // make role mentionable
            .flatMap(v -> channel.sendMessage(message))         // send announcement
            .flatMap(m -> {
                if (channel.isNews())
                    message.crosspost().queue();                // publish if it's a news channel
                return role.getManager().setMentionable(false); // make role unmentionable
            })
            .queue();
    }
}

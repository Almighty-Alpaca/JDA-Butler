package com.kantenkugel.discordbot.fakebutler;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.NotifyCommand;
import com.almightyalpaca.discord.jdabutler.commands.commands.StatsCommand;
import com.almightyalpaca.discord.jdabutler.util.DurationUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.Presence;

import javax.annotation.Nonnull;

public class FakeButlerListener implements EventListener
{
    private static final long REAL_BUTLER_ID = 189074312974696448L;
    private static final long JDA_SERVER_ID = 125227483518861312L;

    private long onlineTime;
    private long offlineTime;
    private long latestStamp;
    private boolean online = false;

    public String getStats(JDA jda)
    {
        long on = onlineTime, off = offlineTime;
        if(online)
            on += System.currentTimeMillis() - latestStamp;
        else
            off += System.currentTimeMillis() - latestStamp;
        long sum = on + off;

        return String.format("%s stats since start of %s:\nOnline: %s (%.1f%%)\nOffline: %s (%.1f%%)",
                jda.getUserById(REAL_BUTLER_ID), jda.getSelfUser(),
                DurationUtils.formatDuration(on), on*100f/sum,
                DurationUtils.formatDuration(off), off*100f/sum);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent event)
    {
        if (event instanceof UserUpdateOnlineStatusEvent)
        {
            UserUpdateOnlineStatusEvent e = (UserUpdateOnlineStatusEvent) event;
            Guild guild = e.getGuild();
            User user = e.getUser();
            if(user.getIdLong() == REAL_BUTLER_ID && guild.getIdLong() == JDA_SERVER_ID)
                handleStatus(e.getJDA(), guild.getMember(user));
        }
        else if (event instanceof ReadyEvent)
        {
            if(event.getJDA().getSelfUser().getIdLong() == REAL_BUTLER_ID)
            {
                event.getJDA().removeEventListener(this);
                return;
            }

            Bot.dispatcher.registerCommand(new StatsCommand(this));

            Guild jdaGuild = event.getJDA().getGuildById(JDA_SERVER_ID);
            if (jdaGuild == null)
            {
                handleStatus(event.getJDA(), null);
                return;
            }
            Member butler = jdaGuild.getMemberById(REAL_BUTLER_ID);
            handleStatus(event.getJDA(), butler);
            latestStamp = System.currentTimeMillis();
            onlineTime = offlineTime = 0L;
        }
    }

    private void handleStatus(JDA jda, Member butler)
    {
        Presence presence = jda.getPresence();
        if (butler == null || butler.getOnlineStatus() == OnlineStatus.OFFLINE)
        {
            //Main Butler is offline
            if(presence.getStatus() == OnlineStatus.ONLINE)
                return;
            NotifyCommand.reloadBlacklist(null);
            online = false;
            onlineTime += (System.currentTimeMillis() - latestStamp);
            presence.setStatus(OnlineStatus.ONLINE);
        }
        else
        {
            //Main Butler is online
            if(presence.getStatus() == OnlineStatus.INVISIBLE)
                return;
            online = true;
            offlineTime += (System.currentTimeMillis() - latestStamp);
            presence.setStatus(OnlineStatus.INVISIBLE);
        }
        //Only called if change occurred (or if main butler is online on startup)
        latestStamp = System.currentTimeMillis();

        Bot.isStealth = online;
    }
}

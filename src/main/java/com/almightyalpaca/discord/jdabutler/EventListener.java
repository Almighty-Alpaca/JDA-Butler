package com.almightyalpaca.discord.jdabutler;

import com.almightyalpaca.discord.jdabutler.eval.Engine;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class EventListener extends ListenerAdapter
{

    static final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, (r) ->
    {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) -> throwable.printStackTrace());
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    });

    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event)
    {
        final Guild guild = event.getGuild();
        if (guild.getId().equals("125227483518861312"))
        {
            final Member member = event.getMember();
            final User user = member.getUser();
            Role role;
            if (user.isBot())
                role = Bot.getRoleBots();
            else
                role = Bot.getRoleJdaFanclub();

            final AuditableRestAction<Void> action = guild.getController().addSingleRoleToMember(member, role).reason("Auto Role");
            final String message = String.format("Added %#s (%d) to %s", user, user.getIdLong(), role.getName());
            action.queue(v -> Bot.LOG.info(message), ex -> Bot.LOG.error("Could not add User {} to role {}", user.getName(), role.getName(), ex));
        }
    }

    @Override
    public void onShutdown(final ShutdownEvent event)
    {
        EventListener.executor.shutdown();
        Engine.shutdown();
    }
}

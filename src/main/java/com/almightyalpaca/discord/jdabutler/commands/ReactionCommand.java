package com.almightyalpaca.discord.jdabutler.commands;

import gnu.trove.map.TLongObjectMap;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class ReactionCommand implements Command
{

    public final static String[] NUMBERS = new String[]{"1\u20E3", "2\u20E3", "3\u20E3",
            "4\u20E3", "5\u20E3", "6\u20E3", "7\u20E3", "8\u20E3", "9\u20E3", "\uD83D\uDD1F"};
    public final static String[] LETTERS = new String[]{"\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8",
            "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF"};
    public final static String LEFT_ARROW = "\u2B05";
    public final static String RIGHT_ARROW = "\u27A1";
    public final static String CANCEL = "\u274C";

    private final Dispatcher.ReactionListenerRegistry listenerRegistry;

    public ReactionCommand(Dispatcher.ReactionListenerRegistry registry)
    {
        this.listenerRegistry = registry;
    }

    protected final void addReactions(Message message, List<String> reactions, Set<User> allowedUsers,
                                      int timeout, TimeUnit timeUnit, Consumer<Integer> callback)
    {
        if (!ReactionListener.instances.containsKey(message.getIdLong()))
            new ReactionListener(message, reactions, allowedUsers, listenerRegistry, timeout, timeUnit, callback);
    }

    protected final void stopReactions(Message message)
    {
        stopReactions(message, true);
    }

    protected final void stopReactions(Message message, boolean removeReactions)
    {
        ReactionListener reactionListener = ReactionListener.instances.get(message.getIdLong());
        if (reactionListener != null)
            reactionListener.stop(removeReactions);
    }

    public static final class ReactionListener
    {
        private static final TLongObjectMap<ReactionListener> instances = MiscUtil.newLongMap();
        private final Message message;
        private final List<String> allowedReactions;
        private final Set<User> allowedUsers;
        private final Dispatcher.ReactionListenerRegistry registry;
        private final Consumer<Integer> callback;
        private final Thread timeoutThread;

        private boolean shouldDeleteReactions = true;

        public ReactionListener(Message message, List<String> allowedReactions, Set<User> allowedUsers,
                                Dispatcher.ReactionListenerRegistry registry, int timeout, TimeUnit timeUnit,
                                Consumer<Integer> callback)
        {
            instances.put(message.getIdLong(), this);
            this.message = message;
            this.allowedReactions = allowedReactions;
            this.allowedUsers = allowedUsers;
            this.registry = registry;
            this.callback = callback;
            this.timeoutThread = new Thread(new TimeoutHandler(timeout, timeUnit));
            this.timeoutThread.start();
            addReactions();
            registry.register(this);
        }

        public void handle(MessageReactionAddEvent event)
        {
            if (event.getMessageIdLong() != message.getIdLong())
                return;
            if (event.getUser() == event.getJDA().getSelfUser())
                return;

            try
            {
                event.getReaction().removeReaction(event.getUser()).queue();
            } catch (PermissionException ignored) {}

            if (!allowedUsers.isEmpty() && !allowedUsers.contains(event.getUser()))
                return;

            MessageReaction.ReactionEmote reactionEmote = event.getReactionEmote();
            String reaction = reactionEmote.isEmote() ? reactionEmote.getEmote().getId() : reactionEmote.getName();
            if (allowedReactions.contains(reaction))
                callback.accept(allowedReactions.indexOf(reaction));
        }

        private void stop(boolean removeReactions)
        {
            this.shouldDeleteReactions = removeReactions;
            this.timeoutThread.interrupt();
        }

        private void addReactions()
        {
            if (message.getChannelType() == ChannelType.TEXT && !message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION))
                return;
            for (String reaction : allowedReactions)
            {
                Emote emote = null;
                try
                {
                    emote = message.getJDA().getEmoteById(reaction);
                } catch (NumberFormatException ignored) {}
                if (emote == null)
                {
                    message.addReaction(reaction).queue();
                }
                else
                {
                    message.addReaction(emote).queue();
                }
            }
        }

        private void cleanup()
        {
            registry.remove(ReactionListener.this);
            if (shouldDeleteReactions)
            {
                try
                {
                    message.clearReactions().queue();
                } catch (PermissionException ignored) {}
            }
            instances.remove(message.getIdLong());
        }

        private final class TimeoutHandler implements Runnable
        {
            private final int timeout;
            private final TimeUnit timeUnit;

            private TimeoutHandler(int timeout, TimeUnit timeUnit)
            {
                this.timeout = timeout;
                this.timeUnit = timeUnit;
            }

            @Override
            public void run()
            {
                try
                {
                    timeUnit.sleep(timeout);
                } catch (InterruptedException ignored) {}
                cleanup();
            }
        }
    }
}

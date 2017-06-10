package com.almightyalpaca.discord.jdabutler.commands;

import gnu.trove.map.TLongObjectMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.MiscUtil;

public abstract class ReactionCommand implements Command
{

    public final static String CANCEL = "\u274C";
    public final static String[] LETTERS = new String[]
    { "\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF" };
    public final static String[] NUMBERS = new String[]
    { "1\u20E3", "2\u20E3", "3\u20E3", "4\u20E3", "5\u20E3", "6\u20E3", "7\u20E3", "8\u20E3", "9\u20E3", "\uD83D\uDD1F" };

    private final Dispatcher.ReactionListenerRegistry listenerRegistry;

    public ReactionCommand(final Dispatcher.ReactionListenerRegistry registry)
    {
        this.listenerRegistry = registry;
    }

    protected final void addReactions(final Message message, final List<String> reactions, final Set<User> allowedUsers, final int timeout, final TimeUnit timeUnit, final Consumer<Integer> callback)
    {
        if (!ReactionListener.instances.containsKey(message.getIdLong()))
            new ReactionListener(message, reactions, allowedUsers, this.listenerRegistry, timeout, timeUnit, callback);
    }

    protected final void stopReactions(final Message message)
    {
        this.stopReactions(message, true);
    }

    protected final void stopReactions(final Message message, final boolean removeReactions)
    {
        final ReactionListener reactionListener = ReactionListener.instances.get(message.getIdLong());
        if (reactionListener != null)
            reactionListener.stop(removeReactions);
    }

    public static final class ReactionListener
    {
        private static final TLongObjectMap<ReactionListener> instances = MiscUtil.newLongMap();
        private final List<String> allowedReactions;
        private final Set<User> allowedUsers;
        private final Consumer<Integer> callback;
        private final Message message;
        private final Dispatcher.ReactionListenerRegistry registry;
        private boolean shouldDeleteReactions = true;

        private final Thread timeoutThread;

        public ReactionListener(final Message message, final List<String> allowedReactions, final Set<User> allowedUsers, final Dispatcher.ReactionListenerRegistry registry, final int timeout, final TimeUnit timeUnit, final Consumer<Integer> callback)
        {
            ReactionListener.instances.put(message.getIdLong(), this);
            this.message = message;
            this.allowedReactions = allowedReactions;
            this.allowedUsers = allowedUsers;
            this.registry = registry;
            this.callback = callback;
            this.timeoutThread = new Thread(new TimeoutHandler(timeout, timeUnit));
            this.timeoutThread.start();
            this.addReactions();
            registry.register(this);
        }

        public void handle(final MessageReactionAddEvent event)
        {
            if (event.getMessageIdLong() != this.message.getIdLong())
                return;
            if (event.getUser() == event.getJDA().getSelfUser())
                return;

            try
            {
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            catch (final PermissionException ignored)
            {}

            if (!this.allowedUsers.isEmpty() && !this.allowedUsers.contains(event.getUser()))
                return;

            final MessageReaction.ReactionEmote reactionEmote = event.getReactionEmote();
            final String reaction = reactionEmote.isEmote() ? reactionEmote.getEmote().getId() : reactionEmote.getName();
            if (this.allowedReactions.contains(reaction))
                this.callback.accept(this.allowedReactions.indexOf(reaction));
        }

        private void addReactions()
        {
            if (this.message.getChannelType() == ChannelType.TEXT && !this.message.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION))
                return;
            for (final String reaction : this.allowedReactions)
            {
                Emote emote = null;
                try
                {
                    emote = this.message.getJDA().getEmoteById(reaction);
                }
                catch (final NumberFormatException ignored)
                {}
                if (emote == null)
                    this.message.addReaction(reaction).queue();
                else
                    this.message.addReaction(emote).queue();
            }
        }

        private void cleanup()
        {
            this.registry.remove(ReactionListener.this);
            if (this.shouldDeleteReactions)
                try
                {
                    this.message.clearReactions().queue();
                }
                catch (final PermissionException ignored)
                {}
            ReactionListener.instances.remove(this.message.getIdLong());
        }

        private void stop(final boolean removeReactions)
        {
            this.shouldDeleteReactions = removeReactions;
            this.timeoutThread.interrupt();
        }

        private final class TimeoutHandler implements Runnable
        {
            private final int timeout;
            private final TimeUnit timeUnit;

            private TimeoutHandler(final int timeout, final TimeUnit timeUnit)
            {
                this.timeout = timeout;
                this.timeUnit = timeUnit;
            }

            @Override
            public void run()
            {
                try
                {
                    this.timeUnit.sleep(this.timeout);
                }
                catch (final InterruptedException ignored)
                {}
                ReactionListener.this.cleanup();
            }
        }
    }
}

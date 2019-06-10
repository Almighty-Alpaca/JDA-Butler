package com.almightyalpaca.discord.jdabutler.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class ReactionCommand extends Command
{
    public final static String[] NUMBERS = new String[]{"1\u20E3", "2\u20E3", "3\u20E3",
            "4\u20E3", "5\u20E3", "6\u20E3", "7\u20E3", "8\u20E3", "9\u20E3", "\uD83D\uDD1F"};
    public final static String[] LETTERS = new String[]{"\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8",
            "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF"};
    public final static String LEFT_ARROW = "\u2B05";
    public final static String RIGHT_ARROW = "\u27A1";
    public final static String CANCEL = "\u274C";

    private final ReactionListenerRegistry listenerRegistry;

    public ReactionCommand(ReactionListenerRegistry registry)
    {
        this.listenerRegistry = registry;
    }

    protected final void addReactions(Message message, List<String> reactions, Set<User> allowedUsers,
                                      int timeout, TimeUnit timeUnit, Consumer<Integer> callback)
    {
        if (!listenerRegistry.hasListener(message.getIdLong()))
            listenerRegistry.newListener(message, reactions, allowedUsers, timeout, timeUnit, callback);
    }

    protected final void stopReactions(Message message)
    {
        stopReactions(message, true);
    }

    protected final void stopReactions(Message message, boolean removeReactions)
    {
        listenerRegistry.cancel(message.getIdLong(), removeReactions);
    }
}

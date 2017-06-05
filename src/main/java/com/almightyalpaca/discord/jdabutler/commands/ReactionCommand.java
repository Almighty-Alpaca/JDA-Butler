package com.almightyalpaca.discord.jdabutler.commands;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class ReactionCommand implements Command {
    private final Dispatcher.ReactionListenerRegistry listenerRegistry;

    public ReactionCommand(Dispatcher.ReactionListenerRegistry registry) {
        this.listenerRegistry = registry;
    }

    protected final void addReactions(Message message, List<String> reactions, int timeout, TimeUnit timeUnit, Consumer<Integer> callback) {
        if(!ReactionListener.instances.containsKey(message.getIdLong()))
            new ReactionListener(message, reactions, listenerRegistry, timeout, timeUnit, callback);
    }

    protected final void stopReactions(Message message) {
        ReactionListener reactionListener = ReactionListener.instances.get(message.getIdLong());
        if(reactionListener != null)
            reactionListener.stop();
    }

    public static final class ReactionListener {
        private static final Map<Long, ReactionListener> instances = new HashMap<>();
        private final Message message;
        private final List<String> allowedReactions;
        private final Dispatcher.ReactionListenerRegistry registry;
        private final Consumer<Integer> callback;
        private final Thread timeoutThread;

        public ReactionListener(Message message, List<String> allowedReactions, Dispatcher.ReactionListenerRegistry registry, int timeout, TimeUnit timeUnit, Consumer<Integer> callback) {
            instances.put(message.getIdLong(), this);
            this.message = message;
            this.allowedReactions = allowedReactions;
            this.registry = registry;
            this.callback = callback;
            this.timeoutThread = new Thread(new TimeoutHandler(timeout, timeUnit));
            this.timeoutThread.start();
            registry.register(this);
        }

        public void handle(MessageReactionAddEvent event) {
            if(event.getMessageIdLong() != message.getIdLong())
                return;
            if(event.getUser() == event.getJDA().getSelfUser())
                return;
            String name = event.getReactionEmote().getName();
            if(!allowedReactions.contains(name)) {
                event.getReaction().removeReaction(event.getUser());
                return;
            }
            callback.accept(allowedReactions.indexOf(name));
        }

        private void stop() {
            this.timeoutThread.interrupt();
        }

        private void cleanup() {
            registry.remove(ReactionListener.this);
            message.clearReactions();
            instances.remove(message.getIdLong());
        }

        private final class TimeoutHandler implements Runnable {
            private final int timeout;
            private final TimeUnit timeUnit;

            private TimeoutHandler(int timeout, TimeUnit timeUnit) {
                this.timeout = timeout;
                this.timeUnit = timeUnit;
            }

            @Override
            public void run() {
                try {
                    timeUnit.sleep(timeout);
                } catch(InterruptedException ignored) {}
                cleanup();
            }
        }
    }
}

package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.util.FixedSizeCache;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;

import java.util.function.Consumer;

public class ButtonListener implements EventListener {
    private final FixedSizeCache<String, Consumer<? super ButtonInteraction>> listeners = new FixedSizeCache<>(100);

    @Override
    public void onEvent(GenericEvent event)
    {
        if (event instanceof ButtonClickEvent)
            onButton((ButtonClickEvent) event);
    }

    private void onButton(ButtonClickEvent event)
    {
        Consumer<? super ButtonInteraction> callback = listeners.find(prefix -> event.getComponentId().startsWith(prefix));
        if (callback == null)
        {
            event.reply("This menu timed out!").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        callback.accept(event);
    }

    public void addListener(String prefix, Consumer<? super ButtonInteraction> callback)
    {
        listeners.add(prefix, callback);
    }
}

package com.almightyalpaca.discord.jdabutler.util;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SelectionMenu {
    private final List<Message> options = new ArrayList<>();
    private final List<Button> buttons = new ArrayList<>();
    private final long messageId, userId;
    private int current = 0;

    public SelectionMenu(Message message) {
        this.messageId = message.getIdLong();
        this.userId = message.getAuthor().getIdLong();
    }

    public String getId()
    {
        return messageId + ":" + userId;
    }

    public Message getCurrent()
    {
        return options.get(current);
    }

    public SelectionMenu addOption(ButtonStyle style, String buttonText, Message page)
    {
        options.add(page);
        buttons.add(Button.of(style, getId() + ":" + buttons.size(), buttonText));
        return this;
    }

    public SelectionMenu addOption(ButtonStyle style, Emoji emoji, Message page)
    {
        options.add(page);
        buttons.add(Button.of(style, getId() + ":" + buttons.size(), emoji));
        return this;
    }

    public void onButtonClick(ButtonInteraction interaction)
    {
        if (interaction.getUser().getIdLong() != userId) return;
        String[] id = interaction.getComponentId().split(":");
        if (Long.parseUnsignedLong(id[0]) != messageId) return;
        try {
            int index = Integer.parseInt(id[2]);
            if(index < 0 || index >= options.size()) return;

            current = index;
            interaction.getHook()
                    .editOriginal(options.get(current))
                    .setActionRows(getButtons())
                    .queue();
        } catch(NumberFormatException ignored) {

        }
    }

    public ActionRow getButtons()
    {
        return ActionRow.of(
                IntStream.range(0, buttons.size())
                        .mapToObj(i -> buttons.get(i).withDisabled(i == current))
                        .collect(Collectors.toList()).toArray(new Button[buttons.size()])
        );
    }
}

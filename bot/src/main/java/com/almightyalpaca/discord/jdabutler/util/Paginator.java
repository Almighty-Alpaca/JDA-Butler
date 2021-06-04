package com.almightyalpaca.discord.jdabutler.util;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;

import java.util.ArrayList;
import java.util.List;

public class Paginator {
    private final List<Message> pages = new ArrayList<>();
    private final long messageId, userId;
    private int index = 0;

    public Paginator(Message message) {
        this.messageId = message.getIdLong();
        this.userId = message.getAuthor().getIdLong();
    }

    public String getId()
    {
        return messageId + ":" + userId;
    }

    private boolean isEnd()
    {
        return index == pages.size() - 1;
    }

    private boolean isStart()
    {
        return index == 0;
    }

    private Message getNext()
    {
        return isEnd() ? pages.get(index) : pages.get(++index);
    }

    private Message getPrev()
    {
        return isStart() ? pages.get(0) : pages.get(--index);
    }

    public Message getCurrent()
    {
        return pages.get(index);
    }

    public Paginator addPage(Message page)
    {
        pages.add(page);
        return this;
    }

    public void onButtonClick(ButtonInteraction interaction)
    {
        if (interaction.getUser().getIdLong() != userId) return;
        String[] id = interaction.getComponentId().split(":");
        if (Long.parseUnsignedLong(id[0]) != messageId) return;

        String operation = id[2];
        switch (operation)
        {
            case "next":
                interaction.getHook()
                    .editOriginal(getNext())
                    .setActionRows(getButtons())
                    .queue();
                break;
            case "prev":
                interaction.getHook()
                    .editOriginal(getPrev())
                    .setActionRows(getButtons())
                    .queue();
                break;
            case "delete":
                interaction.getHook().deleteOriginal().queue();
                break;
        }
    }

    private Button getNextButton()
    {
        return Button.secondary(getId() + ":next", Emoji.fromUnicode("➡️")); // :arrow_right:
    }

    private Button getPrevButton()
    {
        return Button.secondary(getId() + ":prev", Emoji.fromUnicode("⬅️")); // :arrow_left:
    }

    private Button getDeleteButton()
    {
        return Button.danger(getId() + ":delete", Emoji.fromUnicode("\uD83D\uDEAE")); // :put_litter_in_its_place:
    }

    public ActionRow getButtons()
    {
        return ActionRow.of(
            getPrevButton().withDisabled(isStart()),
            getNextButton().withDisabled(isEnd()),
            getDeleteButton()
        );
    }
}

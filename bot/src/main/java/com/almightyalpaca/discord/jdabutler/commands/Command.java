package com.almightyalpaca.discord.jdabutler.commands;

import com.almightyalpaca.discord.jdabutler.util.FixedSizeCache;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.function.Consumer;

public abstract class Command
{
    private static final FixedSizeCache<Long, TLongSet> MESSAGE_LINK_MAP = new FixedSizeCache<>(20);

    static void removeResponses(TextChannel channel, long messageId, ReactionListenerRegistry reactionRegistry)
    {
        TLongSet responses = MESSAGE_LINK_MAP.get(messageId);
        if(responses != null)
        {
            //if the responses had a reaction listener attached, clear it (cancel() is NOP if the id is not registered)
            responses.forEach(msgId ->
            {
                reactionRegistry.cancel(msgId);
                return true;
            });
            channel.purgeMessagesById(responses.toArray());
        }
    }

    public static void linkMessage(long commandId, long responseId)
    {
        TLongSet set;
        if(!MESSAGE_LINK_MAP.contains(commandId))
        {
            set = new TLongHashSet(2);
            MESSAGE_LINK_MAP.add(commandId, set);
        }
        else
        {
            set = MESSAGE_LINK_MAP.get(commandId);
        }
        set.add(responseId);
    }

    public abstract void dispatch(User sender, TextChannel channel, Message message, String content, GuildMessageReceivedEvent event);

    public String[] getAliases()
    {
        return new String[0];
    }

    public abstract String getHelp();

    public abstract String getName();

    protected void sendFailed(final Message message)
    {
        message.addReaction("❌").queue();
    }

    protected void sendSuccess(final Message message)
    {
        message.addReaction("✅").queue();
    }

    protected void reply(GuildMessageReceivedEvent event, String message)
    {
        reply(event, message, null);
    }

    protected void reply(GuildMessageReceivedEvent event, String message, Consumer<Message> successConsumer)
    {
        reply(event, new MessageBuilder(message).build(), successConsumer);
    }

    protected void reply(GuildMessageReceivedEvent event, MessageEmbed embed)
    {
        reply(event, embed, null);
    }

    protected void reply(GuildMessageReceivedEvent event, MessageEmbed embed, Consumer<Message> successConsumer)
    {
        reply(event, new MessageBuilder(embed).build(), successConsumer);
    }

    protected void reply(GuildMessageReceivedEvent event, Message message)
    {
        reply(event, message, null);
    }

    protected void reply(GuildMessageReceivedEvent event, Message message, Consumer<Message> successConsumer)
    {
        event.getChannel().sendMessage(message).queue(linkReply(event, successConsumer));
    }

    protected Consumer<Message> linkReply(GuildMessageReceivedEvent event, Consumer<Message> successConsumer) {
        return msg ->
        {
            linkMessage(event.getMessageIdLong(), msg.getIdLong());
            if (successConsumer != null)
                successConsumer.accept(msg);
        };
    }
}

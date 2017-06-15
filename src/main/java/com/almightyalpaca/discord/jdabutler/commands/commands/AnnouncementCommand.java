package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.time.OffsetDateTime;

public class AnnouncementCommand implements Command
{

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) throws Exception
    {
        if (!Bot.isHelper(sender))
        {
            this.sendFailed(message);
            return;
        }

        final String[] args = content.split("\\|", 2);

        if (args.length < 2)
        {
            this.sendFailed(message);
            return;
        }

        Role role;
        String image;

        if (channel.equals(Bot.getChannelAnnouncements()))
        {
            role = Bot.getRoleJdaUpdates();
            image = EmbedUtil.JDA_ICON;
        }
        else if (channel.equals(Bot.getChannelLavaplayer()))
        {
            role = Bot.getRoleLavaplayerUpdates();
            image = null;
        }
        else if (channel.equals(Bot.getChannelExperimental()))
        {
            role = Bot.getRoleExperimentalUpdates();
            image = EmbedUtil.JDA_ICON;
        }
        else
        {
            this.sendFailed(message);
            return;
        }

        message.delete().queue();

        final MessageBuilder mb = new MessageBuilder().append(role);
        final EmbedBuilder eb = new EmbedBuilder();

        EmbedUtil.setColor(eb);
        eb.setTitle(args[0].trim(), null);
        eb.setDescription(args[1].trim());
        eb.setTimestamp(OffsetDateTime.now());
        eb.setThumbnail(image);
        eb.setFooter(sender.getName(), sender.getEffectiveAvatarUrl());

        mb.setEmbed(eb.build());

        role.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> role.getManager().setMentionable(false).queue()));

    }

    @Override
    public String getHelp()
    {
        return "`announce [title] | [text]`";
    }

    @Override
    public String getName()
    {
        return "announce";
    }
}

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry.EXPERIMENTAL_ITEM;

public class AnnouncementCommand implements Command
{

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if(!channel.getGuild().equals(Bot.getGuildJda()))
        {
            this.sendFailed(message);
            return;
        }

        final String[] args = content.split("\\s\\|\\s", 3);

        if (args.length < 2)
        {
            this.sendFailed(message);
            return;
        }

        final Role role;
        String image = null;
        if(args.length == 3)
        {
            if(args[0].equals("experimental"))
            {
                if(!Bot.isAdmin(sender))
                {
                    this.sendFailed(message);
                    return;
                }
                role = EXPERIMENTAL_ITEM.getAnnouncementRole();
                image = EmbedUtil.JDA_ICON;
            }
            else
            {
                VersionedItem item = VersionCheckerRegistry.getItem(args[0]);
                if(item == null)
                {
                    channel.sendMessage("Item with name " + args[0] + " doesn't exist!").queue();
                    return;
                }
                if(!item.canAnnounce(sender) && !Bot.isAdmin(sender))
                {
                    this.sendFailed(message);
                    return;
                }
                role = item.getAnnouncementRole();
                if(item.getName().equalsIgnoreCase("jda"))
                    image = EmbedUtil.JDA_ICON;
                if(role == null)
                {
                    channel.sendMessage("This item has no announcement role set up!").queue();
                    return;
                }
            }
        }
        else
        {
            List<VersionedItem> items = VersionCheckerRegistry.getVersionedItems().stream()
                    .filter(i -> i.getAnnouncementRoleId() != 0 && i.getAnnouncementChannelId() == channel.getIdLong()
                            && (Bot.isAdmin(sender) || i.canAnnounce(sender)))
                    .collect(Collectors.toList());
            if(channel.getIdLong() == EXPERIMENTAL_ITEM.getAnnouncementChannelId() && Bot.isAdmin(sender))
                items.add(EXPERIMENTAL_ITEM);
            switch(items.size())
            {
                case 0:
                    channel.sendMessage("No roles set up for this channel or you do not have access to them").queue();
                    return;
                case 1:
                    VersionedItem item = items.get(0);
                    role = item.getAnnouncementRole();
                    if(role == null)
                    {
                        channel.sendMessage("Item has invalid role id set up").queue();
                        return;
                    }
                    if(item.getName().equalsIgnoreCase("jda") || item == EXPERIMENTAL_ITEM)
                        image = EmbedUtil.JDA_ICON;
                    break;
                default:
                    channel.sendMessage("Too many roles set up for this channel. You have to manually specify one via command.").queue();
                    return;
            }
        }

        message.delete().queue();

        @SuppressWarnings("ConstantConditions")
        final MessageBuilder mb = new MessageBuilder().append(role.getAsMention());
        final EmbedBuilder eb = new EmbedBuilder();

        EmbedUtil.setColor(eb);
        eb.setTitle(args.length == 3 ? args[1].trim() : args[0].trim(), null);
        eb.setDescription(args.length == 3 ? args[2].trim() : args[1].trim());
        eb.setTimestamp(OffsetDateTime.now());
        eb.setThumbnail(image);
        eb.setFooter(sender.getName(), sender.getEffectiveAvatarUrl());

        mb.setEmbed(eb.build());

        role.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> role.getManager().setMentionable(false).queue()));

    }

    @Override
    public String getHelp()
    {
        return "`announce [ROLE | ]TITLE | TEXT`";
    }

    @Override
    public String getName()
    {
        return "announce";
    }
}

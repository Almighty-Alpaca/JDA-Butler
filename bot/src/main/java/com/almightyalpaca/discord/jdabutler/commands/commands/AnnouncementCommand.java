package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry.EXPERIMENTAL_ITEM;

public class AnnouncementCommand extends Command
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
            reply(event, "Syntax: " + getHelp());
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
                image = EmbedUtil.getJDAIconUrl();
            }
            else
            {
                VersionedItem item = VersionCheckerRegistry.getItem(args[0]);
                if(item == null)
                {
                    reply(event, "Item with name " + args[0] + " doesn't exist!");
                    return;
                }
                if(!item.canAnnounce(sender) && !Bot.isAdmin(sender))
                {
                    this.sendFailed(message);
                    return;
                }
                role = item.getAnnouncementRole();
                if(item.getName().equalsIgnoreCase("jda"))
                    image = EmbedUtil.getJDAIconUrl();
                if(role == null)
                {
                    reply(event, "This item has no announcement role set up!");
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
                    reply(event, "No roles set up for this channel or you do not have access to them");
                    return;
                case 1:
                    VersionedItem item = items.get(0);
                    role = item.getAnnouncementRole();
                    if(role == null)
                    {
                        reply(event, "Item has invalid role id set up");
                        return;
                    }
                    if(item.getName().equalsIgnoreCase("jda") || item == EXPERIMENTAL_ITEM)
                        image = EmbedUtil.getJDAIconUrl();
                    break;
                default:
                    reply(event, "Too many roles set up for this channel. You have to manually specify one via command.");
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

        mb.setEmbeds(eb.build());

        MiscUtils.announce(channel, role, mb.build(), true);

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

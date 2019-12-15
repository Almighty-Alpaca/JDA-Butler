package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class NotifyCommand extends Command
{

    private static final long BLACKLIST_CHANNEL_ID = 454657809397710859L;
    private static final String[] ALIASES = { "subscribe" };

    private static final TLongSet BLACKLIST = new TLongHashSet();

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final Member member = channel.getGuild().getMember(sender);
        final Guild guild = channel.getGuild();

        if (!guild.equals(Bot.getGuildJda()))
        {
            this.sendFailed(message);
            return;
        }

        if(content.startsWith("blacklist"))
        {
            if(Bot.isAdmin(sender))
            {
                String subContent = content.substring(Math.min("blacklist".length() + 1, content.length()));
                handleBlacklist(event, message, subContent);
            }
            else
            {
                sendFailed(message);
            }
            return;
        }
        else if(content.equalsIgnoreCase("list"))
        {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setDescription("You can subscribe to one of the following items. To unsubscribe simply type the command again.");
            VersionCheckerRegistry.getVersionedItems().stream()
                .filter(item -> item.getAnnouncementRole() != null)
                .map(item ->
                    new MessageEmbed.Field(
                        item.getName().toUpperCase(),
                        item.getDescription() +
                            "\nCommand: `!notify " + item.getName().toLowerCase() + "`",
                        false
                    )
                ).forEach(embed::addField);

            channel.sendMessage(embed.build()).queue();
            return;
        }

        if(BLACKLIST.contains(sender.getIdLong()))
        {
            message.addReaction("\uD83D\uDE49").queue();
            return;
        }

        final List<Role> roles;
        if(content.trim().isEmpty())
        {
            roles = VersionCheckerRegistry.getVersionedItems().stream()
                    .filter(item -> item.getAnnouncementRole() != null && item.getAnnouncementChannelId() == channel.getIdLong())
                    .map(VersionedItem::getAnnouncementRole)
                    .distinct() //just in case 2 items use same announcement role
                    .collect(Collectors.toList());
            if(roles.size() == 0)
                respond(message, "No role(s) set up for this channel");
        }
        else
        {
            roles = VersionCheckerRegistry.getItemsFromString(content, false).stream()
                    .map(VersionedItem::getAnnouncementRole)
                    .filter(Objects::nonNull)
                    .distinct() //just in case 2 items use same announcement role
                    .collect(Collectors.toList());
            if(content.contains("experimental"))
                roles.add(VersionCheckerRegistry.EXPERIMENTAL_ITEM.getAnnouncementRole());
            if(roles.size() == 0)
                respond(message, "No role(s) found for query");
        }

        if(roles.size() == 0)
            return;

        List<Role> missingRoles = roles.stream().filter(r -> !member.getRoles().contains(r)).collect(Collectors.toList());
        if(missingRoles.size() > 0)
        {
            guild.modifyMemberRoles(member, missingRoles, null).reason("Notify command").queue(vd ->
            {
                logRoleAddition(sender, missingRoles);
                respond(message, "Added you to role(s) "+getRoleListString(missingRoles));
            }, e ->
            {
                Bot.LOG.error("Could not add role(s) to user {}", sender.getIdLong(), e);
                respond(message, "There was an error adding roles. Please notify the devs.");
            });
        }
        else
        {
            guild.modifyMemberRoles(member, null, roles).reason("Notify command").queue(vd ->
            {
                logRoleRemoval(sender, roles);
                respond(message, "Removed you from role(s) "+getRoleListString(roles));
            }, e ->
            {
                Bot.LOG.error("Could not remove role(s) from user {}", sender.getIdLong(), e);
                respond(message, "There was an error removing roles. Please notify the devs.");
            });
        }
    }

    private static void logRoleRemoval(final User sender, final List<Role> roles)
    {
        final String msg = String.format("Removed %#s (%d) from %s", sender, sender.getIdLong(), getRoleListString(roles));
        Bot.LOG.info(msg);
    }

    private static void logRoleAddition(final User sender, final List<Role> roles)
    {
        final String msg = String.format("Added %#s (%d) to %s", sender, sender.getIdLong(), getRoleListString(roles));
        Bot.LOG.info(msg);
    }

    private static String getRoleListString(List<Role> roles)
    {
        return roles.stream().map(r -> '`' + r.getName() + '`').collect(Collectors.joining(", "));
    }

    @Override
    public String[] getAliases()
    {
        return NotifyCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Notifies you about updates. Usage: `!notify [item...]` or `!notify list` for a list of subscription options.";
    }

    @Override
    public String getName()
    {
        return "notify";
    }

    public static void reloadBlacklist(GuildMessageReceivedEvent event)
    {
        TextChannel blacklistChannel = getBlacklistChannel();
        BLACKLIST.clear();
        blacklistChannel.getIterableHistory().forEachAsync(message ->
        {
            String[] split = message.getContentRaw().split("\\s+");
            try
            {
                long userId = Long.parseUnsignedLong(split[0]);
                BLACKLIST.add(userId);
            }
            catch(NumberFormatException ex)
            {
                if(event != null)
                    event.getChannel().sendMessageFormat("Message `%s` is not a valid blacklist message", message.getContentStripped())
                            .queue(msg -> linkMessage(event.getMessageIdLong(), msg.getIdLong()));
            }
            return true;
        }).thenRun(() ->
        {
            if(event != null)
                event.getChannel().sendMessage("Reloaded " + BLACKLIST.size() + " users into blacklist").queue(msg ->
                        linkMessage(event.getMessageIdLong(), msg.getIdLong()));
        });
    }

    private void handleBlacklist(GuildMessageReceivedEvent event, Message msg, String content)
    {
        if(content.isEmpty())
        {
            sendFailed(msg);
            return;
        }
        String[] args = content.split("\\s+", 2);
        TextChannel blacklistChannel = getBlacklistChannel();
        switch(args[0].toLowerCase())
        {
            case "fetch":
            case "generate":
            case "get":
                TextChannel searchChannel = msg.getMentionedChannels().isEmpty()
                        ? VersionCheckerRegistry.getItem("jda").getAnnouncementChannel()
                        : msg.getMentionedChannels().get(0);
                if(searchChannel == null)
                    reply(event, "Could not determine channel to search in");
                else
                    fetchBlacklist(searchChannel, event);
                break;
            case "update":
            case "import":
            case "reload":
                reloadBlacklist(event);
                break;
            case "add":
                msg.getMentionedUsers().forEach(u ->
                {
                    if(!BLACKLIST.contains(u.getIdLong()))
                    {
                        BLACKLIST.add(u.getIdLong());
                        sendBlacklistAdditionMessage(blacklistChannel, u);
                    }
                });
                msg.addReaction("\u2705").queue();
                break;
            case "rm":
            case "remove":
                TLongSet removedIds = new TLongHashSet();
                msg.getMentionedUsers().forEach(u ->
                {
                    if(BLACKLIST.contains(u.getIdLong()))
                    {
                        BLACKLIST.remove(u.getIdLong());
                        removedIds.add(u.getIdLong());
                    }
                });
                removeFromChannel(blacklistChannel, removedIds);
                msg.addReaction("\u2705").queue();
                break;
            default:
                reply(event, "Unknown subcommand");
        }
    }

    private void fetchBlacklist(TextChannel searchChannel, GuildMessageReceivedEvent event)
    {
        Message mentionMessage = searchChannel.getIterableHistory().stream()
                .filter(message -> message.getAuthor().isBot() && !message.getMentionedRoles().isEmpty())
                .limit(500).findFirst().orElse(null);
        if(mentionMessage == null)
        {
            reply(event, "Could not find announcement message within 500 messages");
            return;
        }

        Role announcementRole = mentionMessage.getMentionedRoles().get(0);
        OffsetDateTime abortTime = mentionMessage.getTimeCreated();

        TLongSet blacklistedUsers = new TLongHashSet();

        searchChannel.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).forEachAsync(log ->
        {
            if(log.getTimeCreated().isBefore(abortTime))
                return false;
            AuditLogChange removedRoles = log.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE);
            if(removedRoles == null)
                return true;

            if(log.getUser() == null || log.getUser().isBot() || !Bot.isAdmin(log.getUser()))
                return true;


            List<Map<String, String>> removedRoleMap = removedRoles.getNewValue();
            if(removedRoleMap.stream().mapToLong(map -> Long.parseUnsignedLong(map.get("id"))).noneMatch(rem -> rem == announcementRole.getIdLong()))
                return true;

            blacklistedUsers.add(log.getTargetIdLong());

            return true;
        }).thenRun(() ->
        {
            if(blacklistedUsers.isEmpty() || (blacklistedUsers.removeAll(BLACKLIST) && blacklistedUsers.isEmpty()))
            {
                reply(event, "No1 matching blacklist criteria found!");
                return;
            }
            BLACKLIST.addAll(blacklistedUsers);
            TextChannel blacklistChannel = getBlacklistChannel();
            blacklistedUsers.forEach(userId ->
            {
                sendBlacklistAdditionMessage(blacklistChannel, Bot.jda.getUserById(userId));
                return true;
            });
            reply(event, "Added " + blacklistedUsers.size() + " users to notify blacklist");
        });
    }

    private static void sendBlacklistAdditionMessage(TextChannel blacklistChannel, User blacklisted)
    {
        blacklistChannel.sendMessageFormat("%d - %#s", blacklisted.getIdLong(), blacklisted).queue();
    }

    private static void removeFromChannel(TextChannel blacklistChannel, TLongSet toRemove)
    {
        if(toRemove.isEmpty())
            return;
        blacklistChannel.getIterableHistory().forEachAsync(msg ->
        {
            String[] splits = msg.getContentRaw().split("\\s+");
            try
            {
                long idFromMessage = Long.parseUnsignedLong(splits[0]);
                if(toRemove.contains(idFromMessage))
                {
                    toRemove.remove(idFromMessage);
                    msg.delete().queue();
                    if(toRemove.isEmpty())
                        return false;
                }
            }
            catch(NumberFormatException ignored) {}

            return true;
        });
    }

    private static TextChannel getBlacklistChannel()
    {
        return Bot.jda.getTextChannelById(BLACKLIST_CHANNEL_ID);
    }

    private static void respond(Message origMsg, String newMessageContent)
    {
        origMsg.getChannel().sendMessage(newMessageContent).queue(responseMsg ->
        {
            responseMsg.delete().queueAfter(10, TimeUnit.SECONDS);
            origMsg.delete().queueAfter(10, TimeUnit.SECONDS);
        });
    }
}

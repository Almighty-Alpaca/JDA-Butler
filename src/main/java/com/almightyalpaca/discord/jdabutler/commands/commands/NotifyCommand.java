package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotifyCommand implements Command
{

    private static final String[] ALIASES = { "subscribe" };

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

        final List<Role> roles;
        if(content.trim().isEmpty())
        {
            roles = Collections.singletonList(VersionCheckerRegistry.getItem("jda").getAnnouncementRole());
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
        }

        if(roles.size() == 0)
        {
            channel.sendMessage("No role(s) found for query").queue();
            return;
        }

        List<Role> missingRoles = roles.stream().filter(r -> !member.getRoles().contains(r)).collect(Collectors.toList());
        if(missingRoles.size() > 0)
        {
            guild.getController().addRolesToMember(member, missingRoles).reason("Notify command").queue(vd ->
            {
                logRoleAddition(sender, missingRoles);
                channel.sendMessage("Added you to role(s) "+getRoleListString(missingRoles)).queue();
            }, e ->
            {
                Bot.LOG.error("Could not add role(s) to user {}", sender.getIdLong(), e);
                channel.sendMessage("There was an error adding roles. Please notify the devs.").queue();
            });
        }
        else
        {
            guild.getController().removeRolesFromMember(member, roles).reason("Notify command").queue(vd ->
            {
                logRoleRemoval(sender, roles);
                channel.sendMessage("Removed you from role(s) "+getRoleListString(roles)).queue();
            }, e ->
            {
                Bot.LOG.error("Could not remove role(s) from user {}", sender.getIdLong(), e);
                channel.sendMessage("There was an error removing roles. Please notify the devs.").queue();
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
        return roles.stream().map(Role::getName).collect(Collectors.joining(", "));
    }

    @Override
    public String[] getAliases()
    {
        return NotifyCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Notifies you about updates";
    }

    @Override
    public String getName()
    {
        return "notify";
    }
}

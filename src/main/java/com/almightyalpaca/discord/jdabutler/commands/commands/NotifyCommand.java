package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class NotifyCommand implements Command
{

    private static final String[] ALIASES = new String[]
    { "subscribe" };

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

        if (content.contains("all") || content.contains("both"))
        {
            final List<Role> roles = new ArrayList<>(3);
            roles.add(Bot.getRoleJdaUpdates());
            roles.add(Bot.getRoleLavaplayerUpdates());
            roles.removeAll(member.getRoles());

            if (roles.size() == 0)
            {
                Role[] updateRoles = { Bot.getRoleJdaUpdates(), Bot.getRoleLavaplayerUpdates() };
                guild.getController().removeRolesFromMember(member, updateRoles).queue(v ->
                {
                    logRoleRemoval(sender, Bot.getRoleJdaUpdates());
                    logRoleRemoval(sender, Bot.getRoleLavaplayerUpdates());
                }, e -> Bot.LOG.error("Could not remove role(s) from member {}", member.getUser().getName(), e));
            }
            else
            {
                guild.getController().addRolesToMember(member, roles)
                        .queue(v -> roles.forEach( r -> logRoleAddition(sender, r)),
                                e -> Bot.LOG.error("Could not add role(s) from member {}", member.getUser().getName(), e));
            }

        }
        else
        {
            final Role role;

            if (content.contains("player"))
                role = Bot.getRoleLavaplayerUpdates();
            else if (content.contains("experimental"))
                role = Bot.getRoleExperimentalUpdates();
            else
                role = Bot.getRoleJdaUpdates();

            if (member.getRoles().contains(role))
            {
                guild.getController().removeSingleRoleFromMember(member, role)
                        .queue(v -> logRoleRemoval(sender, role),
                                e -> Bot.LOG.error("Could not remove role from member {}", member.getUser().getName(), e));
            }
            else
            {
                guild.getController().addSingleRoleToMember(member, role)
                        .queue(v -> logRoleAddition(sender, role),
                                e -> Bot.LOG.error("Could not add role from member {}", member.getUser().getName(), e));
            }
        }

        message.addReaction("\uD83D\uDC4C\uD83C\uDFFC").queue();
    }

    private void logRoleRemoval(final User sender, final Role role)
    {
        final String msg = String.format("Removed %#s (%d) from %s", sender, sender.getIdLong(), role.getName());
        Bot.LOG.info(msg);
    }

    private void logRoleAddition(final User sender, final Role role)
    {
        final String msg = String.format("Added %#s (%d) to %s", sender, sender.getIdLong(), role.getName());
        Bot.LOG.info(msg);
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

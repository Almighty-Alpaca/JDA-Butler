package com.almightyalpaca.discord.jdabutler.commands.commands.moderation;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.List;

public class SoftbanCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final Member sendMem = event.getMember();
        if (!sendMem.hasPermission(Permission.KICK_MEMBERS))
        { // only kick cause its not perma ban
            sendFailed(message);
            return;
        }

        if (!channel.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS))
        {
            reply(event, "I am unable to ban members!");
            return;
        }

        final List<User> mentions = message.getMentionedUsers();
        if (mentions.isEmpty())
        {
            reply(event, "Please mention someone");
            return;
        }

        final Guild guild = event.getGuild();
        final GuildController controller = guild.getController();
        final Member self = guild.getSelfMember();

        for (final User user : mentions)
        {
            if (user == null)
                continue;
            final Member member = guild.getMember(user);
            if (member != null && !self.canInteract(member))
                continue;
            final String reason = "Softban by " + sender.getName();
            controller.ban(user, 7).reason(reason).queue((v) -> controller.unban(user).reason(reason).queue());
        }
    }

    @Override
    public String[] getAliases()
    {
        return new String[]
        { "sb" };
    }

    @Override
    public String getHelp()
    {
        return "Kicks user and clears past messages";
    }

    @Override
    public String getName()
    {
        return "softban";
    }
}

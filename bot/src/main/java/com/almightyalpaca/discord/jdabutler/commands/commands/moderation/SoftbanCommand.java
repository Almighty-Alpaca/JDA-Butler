package com.almightyalpaca.discord.jdabutler.commands.commands.moderation;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class SoftbanCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final Member sendMem = event.getMember();
        //noinspection ConstantConditions this is never null here
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

        final List<Member> mentions = message.getMentionedMembers();
        if (mentions.isEmpty())
        {
            reply(event, "Please mention someone");
            return;
        }

        final Guild guild = event.getGuild();
        final Member self = guild.getSelfMember();

        final String reason = "Softban by " + sender.getName();
        for (final Member member : mentions)
        {
            if (!self.canInteract(member))
                continue;

            guild.ban(member.getUser(), 7).reason(reason)
                 .flatMap((v) -> guild.unban(member.getUser()).reason(reason))
                 .queue();
        }
    }

    @Override
    public String[] getAliases()
    {
        return new String[] { "sb" };
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

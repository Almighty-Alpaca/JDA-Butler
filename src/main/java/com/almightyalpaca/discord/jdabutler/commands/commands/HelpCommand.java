package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;

public class HelpCommand implements Command
{
    private static final String[] ALIASES = new String[]
    { "info" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder builder = new EmbedBuilder();
        EmbedUtil.setColor(builder);

        final String prefix = Bot.config.getString("prefix");

        final int size = Bot.dispatcher.getCommands().stream().filter(c -> c.getHelp() != null).map(c -> c.getName().length()).max((i1, i2) -> i1.compareTo(i2)).get() + 1 + prefix.length();

        final String help = Bot.dispatcher.getCommands().stream().sorted(Comparator.comparing(Command::getName)).filter(c -> c.getHelp() != null).map(c -> String.format("`%s` - %s", StringUtils.rightPad(prefix + c.getName().toLowerCase() + "", size, "."), c.getHelp())).collect(Collectors.joining("\n"));
        builder.setAuthor(channel.getGuild().getMember(sender).getEffectiveName(), null, sender.getEffectiveAvatarUrl());
        builder.setDescription(help);
        channel.sendMessage(new MessageBuilder().setEmbed(builder.build()).build()).queue();
    }

    @Override
    public String[] getAliases()
    {
        return HelpCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Prints a list of commands";
    }

    @Override
    public String getName()
    {
        return "help";
    }
}

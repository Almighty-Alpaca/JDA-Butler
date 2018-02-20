package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.GradleUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class BuildGradleCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final MessageBuilder mb = new MessageBuilder();

        VersionedItem jdaItem = VersionCheckerRegistry.getItem("jda");
        LinkedList<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getCustomVersionSupplier() == null)
                .collect(Collectors.toCollection(LinkedList::new));
        //force jda to be at first position
        items.remove(jdaItem);
        items.addFirst(jdaItem);

        final boolean pretty = content.contains("pretty");

        mb.appendCodeBlock(GradleUtil.getBuildFile(GradleUtil.DEFAULT_PLUGINS, "com.example.jda.Bot", "1.0", "1.8", items, pretty), "gradle");
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getHelp()
    {
        return "Shows an example build.gradle file";
    }

    @Override
    public String getName()
    {
        return "build.gradle";
    }
}

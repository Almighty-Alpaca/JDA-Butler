package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.util.gradle.GradleUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class BuildGradleCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final MessageBuilder mb = new MessageBuilder();

        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getGroupId() != null && item.getArtifactId() != null && item.getRepoType() != null)
                .collect(Collectors.toList());

        final boolean pretty = content.contains("pretty");

        mb.appendCodeBlock(GradleUtil.getBuildFile(GradleUtil.DEFAULT_PLUGINS, "com.example.jda.Bot", "1.0", "1.8", items, pretty), "gradle");
        reply(event, mb.build());
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

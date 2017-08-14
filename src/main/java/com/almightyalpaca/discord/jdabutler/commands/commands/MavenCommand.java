package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class MavenCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final MessageBuilder mb = new MessageBuilder();
        final EmbedBuilder eb = new EmbedBuilder();

        List<VersionedItem> deps = new ArrayList<>(3);
        deps.add(VersionChecker.getItem("jda"));

        String author = "Maven dependencies for JDA";
        if (content.contains("player"))
        {
            deps.add(VersionChecker.getItem("lavaplayer"));
            author += " and Lavaplayer";
        }
        if (content.toLowerCase().contains("util"))
        {
            deps.add(VersionChecker.getItem("jda-utilities"));
            author += " and JDA-Utilities";
        }

        eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

        StringBuilder field = new StringBuilder("If you don't know maven type `!pom.xml` for a complete maven build file\n\n```xml\n");

        for (VersionedItem dep : deps)
        {
            field.append(MavenUtil.getDependencyString(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), null)).append("\n");
        }

        field.append("\n");

        deps.stream().map(VersionedItem::getRepoType).distinct().forEachOrdered(repoType -> {
            field.append(MavenUtil.getRepositoryString(repoType.toString(), repoType.toString(), repoType.getRepoBase(), null)).append("\n");
        });

        field.append("```");

        eb.addField("", field.toString(), false);

        EmbedUtil.setColor(eb);
        mb.setEmbed(eb.build());
        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String getHelp()
    {
        return "Shows maven dependency information";
    }

    @Override
    public String getName()
    {
        return "maven";
    }
}

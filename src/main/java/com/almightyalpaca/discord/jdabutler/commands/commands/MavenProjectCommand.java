package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.MavenUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class MavenProjectCommand implements Command
{
    private static String POM;

    static
    {
        try
        {
            MavenProjectCommand.POM = new BufferedReader(new InputStreamReader(MavenProjectCommand.class.getResourceAsStream("/maven.pom"))).lines().collect(Collectors.joining("\n"));
        }
        catch (final Exception e)
        {
            Bot.LOG.error("Error getting template pom", e);
            MavenProjectCommand.POM = "Load failed.";
        }
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        VersionedItem jdaItem = VersionCheckerRegistry.getItem("jda");
        LinkedList<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getCustomVersionSupplier() == null)
                .collect(Collectors.toCollection(LinkedList::new));
        //force jda to be at first position
        items.remove(jdaItem);
        items.addFirst(jdaItem);

        //dependency-string:
        String dependencyString = MavenUtil.getDependencyBlock(items, "    ");

        //repo-string
        String repoString = MavenUtil.getRepositoryBlock(items, "    ");

        final String pom = String.format(MavenProjectCommand.POM, repoString, dependencyString);
        channel.sendMessage("Here: " + Bot.hastebin(pom) + ".xml").queue();
    }

    @Override
    public String getHelp()
    {
        return "Example maven project";
    }

    @Override
    public String getName()
    {
        return "pom.xml";
    }
}

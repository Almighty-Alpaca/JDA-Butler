package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class JarsCommand implements Command
{
    private static final String[] ALIASES = new String[]
    { "jar" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {

        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);
        eb.setAuthor("Latest JDA jars", null, EmbedUtil.JDA_ICON);
        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

        JenkinsBuild lastBuild = JenkinsApi.getLastSuccessfulBuild();

        eb.addField("jar", "[download](" + lastBuild.artifacts.get("jar").getLink() + ")", true);
        eb.addField("javadoc", "[download](" + lastBuild.artifacts.get("javadoc").getLink() + ")", true);
        eb.addField("sources", "[download](" + lastBuild.artifacts.get("sources").getLink() + ")", true);
        eb.addField("withDependencies", "[download](" + lastBuild.artifacts.get("withDependencies").getLink() + ")", true);

        channel.sendMessage(eb.build()).queue();

    }

    @Override
    public String[] getAliases()
    {
        return JarsCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "Displays links to all JAR files";
    }

    @Override
    public String getName()
    {
        return "jars";
    }
}

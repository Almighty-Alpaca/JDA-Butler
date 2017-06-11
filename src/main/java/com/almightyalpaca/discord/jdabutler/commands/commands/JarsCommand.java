package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
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

        final String version = Bot.config.getString("jda.version.name");
        final int build = Bot.config.getInt("jda.version.build");

        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);
        eb.setAuthor("Latest JDA jars", null, EmbedUtil.JDA_ICON);
        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);
        eb.addField("jar", "[download](http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + ".jar)", true);
        eb.addField("javadoc", "[download](http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-javadoc.jar)", true);
        eb.addField("sources", "[download](http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-sources.jar)", true);
        eb.addField("withDependencies", "[download](http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-withDependencies.jar)", true);

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

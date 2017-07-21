package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.GradleProjectDropboxUploader;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class GradleProjectCommand implements Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (!GradleProjectDropboxUploader.GRADLE_PROJECT_ZIP.exists())
            channel.sendTyping().queue();
        GradleProjectDropboxUploader.createZip();

        channel.sendFile(GradleProjectDropboxUploader.GRADLE_PROJECT_ZIP, null).queue();
    }

    @Override
    public String getHelp()
    {
        return "Prints the download link for an up-to-date gradle example project";
    }

    @Override
    public String getName()
    {
        return "gradleproject";
    }
}

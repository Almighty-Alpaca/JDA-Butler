package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.util.gradle.GradleProjectDropboxUtil;
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
        if (GradleProjectDropboxUtil.dropboxShareLink != null)
        {
            channel.sendMessage(GradleProjectDropboxUtil.dropboxShareLink).queue();
        }
        else if (GradleProjectDropboxUtil.ZIP_FILE.exists())
        {
            channel.sendTyping().queue();
            channel.sendFile(GradleProjectDropboxUtil.ZIP_FILE).queue();
        }
        else
        {
            channel.sendMessage("The example zip is currently not available").queue();
        }
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

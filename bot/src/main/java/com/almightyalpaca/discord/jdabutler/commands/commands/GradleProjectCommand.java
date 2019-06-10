package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleProjectDropboxUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GradleProjectCommand extends Command
{
    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (GradleProjectDropboxUtil.dropboxShareLink != null)
        {
            reply(event, GradleProjectDropboxUtil.dropboxShareLink);
        }
        else if (GradleProjectDropboxUtil.ZIP_FILE.exists())
        {
            channel.sendTyping().queue();
            channel.sendFile(GradleProjectDropboxUtil.ZIP_FILE).queue(msg -> linkMessage(message.getIdLong(), msg.getIdLong()));
        }
        else
        {
            reply(event, "The example zip is currently not available");
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

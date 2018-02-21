package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class ChangelogCommand implements Command
{

    private static final String[] ALIASES = { "changes" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder();
        EmbedUtil.setColor(eb);

        VersionedItem item = null;
        int versionStart = 1;
        String[] args = null;
        if(!content.trim().isEmpty())
        {
            args = content.trim().split("[\\s-]", 4);
            item = VersionCheckerRegistry.getItem(args[0]);
        }
        if(item == null)
        {
            versionStart = 0;
            item = VersionCheckerRegistry.getItem("jda");
        }

        ChangelogProvider clProvider = item.getChangelogProvider();

        if(clProvider == null)
        {
            channel.sendMessage("No Changelogs set up for " + item.getName()).queue();
            return;
        }

        //no version parameters or no version lookup supported
        if(!clProvider.supportsIndividualLogs() || args == null || args.length == versionStart)
        {
            channel.sendMessage(String.format("Changelogs for %s can be found here: %s",
                    item.getName(), clProvider.getChangelogUrl())).queue();
            return;
        }

        eb.setTitle("Changelog(s) for " + item.getName(), clProvider.getChangelogUrl());

        //only one version parameter
        if(args.length == versionStart + 1)
        {
            ChangelogProvider.Changelog changelog = clProvider.getChangelog(args[versionStart]);
            if(changelog == null)
            {
                channel.sendMessage("The specified version does not exist").queue();
                return;
            }
            if(changelog.getChangelogUrl() == null)
                eb.appendDescription("**").appendDescription(changelog.getTitle()).appendDescription("**:\n");
            else
                eb.appendDescription("[").appendDescription(changelog.getTitle()).appendDescription("](")
                        .appendDescription(changelog.getChangelogUrl()).appendDescription("):\n");
            if(changelog.getChangeset().isEmpty())
                eb.appendDescription("No changes available for this version");
            else
                eb.appendDescription(String.join("\n", changelog.getChangeset()));
        }
        //more than 1 version given
        else
        {
            List<ChangelogProvider.Changelog> changelogs = clProvider.getChangelogs(args[versionStart], args[versionStart + 1]);
            if(changelogs.size() == 0)
            {
                channel.sendMessage("No Changelogs found in given range").queue();
                return;
            }
            int fields = 0;
            for(ChangelogProvider.Changelog changelog : changelogs) {
                String body = String.join("\n", changelog.getChangeset());
                if(body.length() > MessageEmbed.VALUE_MAX_LENGTH)
                {
                    if(changelog.getChangelogUrl() == null)
                        body = "Too large to show.";
                    else
                        body = "[Link]("+changelog.getChangelogUrl()+") Too large to show.";
                }

                eb.addField(changelog.getTitle(), body, false);

                if(++fields == 19 && changelogs.size() > 20)
                {
                    eb.addField("...", "Embed limit reached. See [Online changelog]("
                            + clProvider.getChangelogUrl() + ')', false);
                }
            }
        }

        MessageEmbed embed = eb.build();
        if(embed.isSendable(AccountType.BOT))
        {
            channel.sendMessage(embed).queue();
        }
        else
        {
            channel.sendMessage("Too much content. Please restrict your build range").queue();
        }
    }

    @Override
    public String[] getAliases()
    {
        return ChangelogCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "`!changelog VERSION [VERSION2]` - Shows changes for some JDA version";
    }

    @Override
    public String getName()
    {
        return "changelog";
    }
}

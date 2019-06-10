package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class ChangelogCommand extends Command
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
            reply(event, "No Changelogs set up for " + item.getName());
            return;
        }

        //no version parameters or no version lookup supported
        if(!clProvider.supportsIndividualLogs() || args == null || args.length == versionStart)
        {
            reply(event, String.format("Changelogs for %s can be found here: %s",
                    item.getName(), clProvider.getChangelogUrl()));
            return;
        }

        eb.setTitle("Changelog(s) for " + item.getName(), clProvider.getChangelogUrl());

        //only one version parameter
        if(args.length == versionStart + 1)
        {
            ChangelogProvider.Changelog changelog = clProvider.getChangelog(args[versionStart]);
            if(changelog == null)
            {
                reply(event, "The specified version does not exist");
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
                reply(event, "No Changelogs found in given range");
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
            reply(event, embed);
        }
        else
        {
            reply(event, "Too much content. Please restrict your build range");
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

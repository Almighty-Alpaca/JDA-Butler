package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.DateUtils;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.changelog.JenkinsChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChangelogCommand extends Command
{

    private static final String[] ALIASES = { "changes" };
    private static final JenkinsApi JENKINS = DateUtils.JENKINS;
    private static final DateTimeFormatter FORMATTER = DateUtils.getDateTimeFormatter();

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

            // Get time of build
            String publishedTime;
            try
            {
                final JenkinsBuild build = args == null
                        ? JENKINS.getLastSuccessfulBuild()
                        : JENKINS.getBuild(Integer.parseInt(args[0]));

                final OffsetDateTime buildTime = build.buildTime;
                publishedTime = FORMATTER.format(buildTime);
            }
            catch (NumberFormatException | IOException ex)
            {
                Bot.LOG.error("Exception in ChangelogCommand occured!", ex);
                publishedTime = "Unable to get Release Time";
            }

            String title;
            if(changelog.getChangelogUrl() == null)
                title = String.format("**%s**", changelog.getTitle());
            else
                title = String.format("[%s](%s)", changelog.getTitle(), changelog.getChangelogUrl());

            final String versionTitle = String.format("%s *(%s)*:\n", title, publishedTime);
            eb.appendDescription(versionTitle);

            if(changelog.getChangeset().isEmpty())
                eb.appendDescription("No changes available for this version");
            else
                eb.appendDescription(String.join("\n", changelog.getChangeset()));
        }
        //more than 1 version given
        else
        {
            final String startVersion = args[versionStart];
            final String endVersion = args[versionStart + 1];

            // get and increment build number instead of fetching it from every changelog to be more reliable
            final int start = JenkinsChangelogProvider.extractBuild(startVersion);
            List<ChangelogProvider.Changelog> changelogs = clProvider.getChangelogs(startVersion, endVersion);
            if(changelogs.size() == 0)
            {
                reply(event, "No Changelogs found in given range");
                return;
            }
            int fields = 0;
            int buildNr = start;
            for(ChangelogProvider.Changelog changelog : changelogs) {
                String body = String.join("\n", changelog.getChangeset());
                if(body.length() > MessageEmbed.VALUE_MAX_LENGTH)
                {
                    if(changelog.getChangelogUrl() == null)
                        body = "Too large to show.";
                    else
                        body = "[Link]("+changelog.getChangelogUrl()+") Too large to show.";
                }

                // Get time of build
                String publishedTime;
                try
                {
                    final JenkinsBuild build = JENKINS.getBuild(buildNr);
                    final OffsetDateTime buildTime = build.buildTime;
                    publishedTime = FORMATTER.format(buildTime);
                }
                catch (IOException ex)
                {
                    Bot.LOG.error("Exception in ChangelogCommand occured!", ex);
                    publishedTime = "Unable to get Release Time";
                }
                eb.addField(String.format("%s (%s)", changelog.getTitle(), publishedTime), body, false);

                if(++fields == 19 && changelogs.size() > 20)
                {
                    eb.addField("...", "Embed limit reached. See [Online changelog]("
                            + clProvider.getChangelogUrl() + ')', false);
                }
                buildNr++;
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

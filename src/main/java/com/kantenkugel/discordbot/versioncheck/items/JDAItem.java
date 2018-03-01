package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.GradleProjectDropboxUploader;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.*;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.changelog.JenkinsChangelogProvider;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class JDAItem extends VersionedItem implements UpdateHandler
{
    private final ChangelogProvider changelogProvider = new JenkinsChangelogProvider(JenkinsApi.JDA_JENKINS, "https://github.com/DV8FromTheWorld/JDA/");

    @Override
    public String getName()
    {
        return "JDA";
    }

    @Override
    public RepoType getRepoType()
    {
        return RepoType.JCENTER;
    }

    @Override
    public String getGroupId()
    {
        return "net.dv8tion";
    }

    @Override
    public String getArtifactId()
    {
        return "JDA";
    }

    @Override
    public String getUrl()
    {
        return JenkinsApi.JDA_JENKINS.getLastSuccessfulBuildUrl();
    }

    @Override
    public long getAnnouncementRoleId()
    {
        return 241948671325765632L;
    }

    @Override
    public long getAnnouncementChannelId()
    {
        return 125227483518861312L;
    }

    @Override
    public UpdateHandler getUpdateHandler()
    {
        return this;
    }

    @Override
    public ChangelogProvider getChangelogProvider()
    {
        return changelogProvider;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onUpdate(VersionedItem item, String previousVersion, boolean shouldAnnounce)
    {
        VersionUtils.VersionSplits versionSplits = item.parseVersion();
        if (versionSplits.build != Bot.config.getInt("jda.version.build", -1))
        {
            Bot.LOG.debug("Update found!");

            Bot.config.put("jda.version.build", versionSplits.build);
            Bot.config.put("jda.version.name", item.getVersion());

            Bot.config.save();

            JenkinsBuild jenkinsBuild = JenkinsApi.JDA_JENKINS.fetchLastSuccessfulBuild();

            if(jenkinsBuild == null)
            {
                Bot.LOG.warn("Could not fetch Jenkins-build for new version (triggered by maven update)");
                return;
            }

            VersionChecker.EXECUTOR.submit(() ->
            {
                JDoc.reFetch();
                GradleProjectDropboxUploader.uploadProject();
            });

            if(!shouldAnnounce)
                return;

            final EmbedBuilder eb = new EmbedBuilder();

            final MessageBuilder mb = new MessageBuilder();

            FormattingUtil.setFooter(eb, jenkinsBuild.culprits, jenkinsBuild.buildTime);

            Role announcementRole = getAnnouncementRole();

            mb.append(announcementRole.getAsMention());

            eb.setAuthor("JDA 3 version " + item.getVersion() + " has been released\n", JenkinsApi.JDA_JENKINS.jenkinsBase + versionSplits.build, EmbedUtil.JDA_ICON);

            EmbedUtil.setColor(eb);

            if (jenkinsBuild.changes.size() > 0)
            {

                eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);
                ChangelogProvider.Changelog changelog = getChangelogProvider().getChangelog(Integer.toString(jenkinsBuild.buildNum));
                List<String> changeset = changelog.getChangeset();

                int fields;

                if (changeset.size() > 25)
                    fields = 24;
                else
                    fields = Math.min(changeset.size(), 25);

                for (int j = 0; j < fields; j++)
                {
                    final String field = changeset.get(j);
                    eb.addField(j == 0 ? "Commits:" : "", field, false);
                }

                if (changeset.size() > 25)
                    eb.addField("", "max embed length reached", false);

            }

            final MessageEmbed embed = eb.build();

            mb.setEmbed(embed);

            mb.build();

            final TextChannel channel = getAnnouncementChannel();

            announcementRole.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> announcementRole.getManager().setMentionable(false).queue()));
        }
    }
}

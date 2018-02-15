package com.kantenkugel.discordbot.versioncheck.updatehandle;

import com.almightyalpaca.discord.jdabutler.*;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public class JDAUpdateHandler implements UpdateHandler
{
    @Override
    public void onUpdate(VersionedItem item)
    {
        VersionedItem.VersionSplits versionSplits = item.parseVersion();
        if (versionSplits.build != Bot.config.getInt("jda.version.build", -1))
        {
            Bot.LOG.debug("Update found!");

            Bot.config.put("jda.version.build", versionSplits.build);
            Bot.config.put("jda.version.name", item.getVersion());

            Bot.config.save();

            JenkinsBuild jenkinsBuild = JenkinsApi.fetchLastSuccessfulBuild();

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

            final EmbedBuilder eb = new EmbedBuilder();

            final MessageBuilder mb = new MessageBuilder();

            FormattingUtil.setFooter(eb, jenkinsBuild.culprits, jenkinsBuild.buildTime);

            mb.append(Bot.getRoleJdaUpdates());

            eb.setAuthor("JDA 3 version " + item.getVersion() + " has been released\n", JenkinsApi.JENKINS_BASE + versionSplits.build, EmbedUtil.JDA_ICON);

            EmbedUtil.setColor(eb);

            if (jenkinsBuild.changes.size() > 0)
            {

                eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

                final List<String> changelog = FormattingUtil.getChangelog(jenkinsBuild.changes);

                int fields;

                if (changelog.size() > 25)
                    fields = 24;
                else
                    fields = Math.min(changelog.size(), 25);

                for (int j = 0; j < fields; j++)
                {
                    final String field = changelog.get(j);
                    eb.addField(j == 0 ? "Commits:" : "", field, false);
                }

                if (changelog.size() > 25)
                    eb.addField("", "max embed length reached", false);

            }

            final MessageEmbed embed = eb.build();

            mb.setEmbed(embed);

            mb.build();

            final Role role = Bot.getRoleJdaUpdates();
            final TextChannel channel = Bot.getChannelAnnouncements();

            role.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> role.getManager().setMentionable(false).queue()));
        }
    }
}
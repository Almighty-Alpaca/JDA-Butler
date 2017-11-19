package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.JDAUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsChange;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Collections;
import java.util.List;

public class ChangelogCommand implements Command
{

    private static final String[] ALIASES = new String[]
    { "changes" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) throws Exception
    {
        final EmbedBuilder eb = new EmbedBuilder().setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

        final List<Integer> buildNums = JDAUtil.getBuildNumbers(content);
        Collections.sort(buildNums);

        final int start;
        final int end;

        if (content.isEmpty())
            start = end = JenkinsApi.getLastSuccessfulBuild().buildNum;
        else if(buildNums.size() == 0)
        {
            channel.sendMessage("Invalid build number(s)").queue();
            return;
        }
        else if (buildNums.size() == 1)
            start = end = buildNums.get(0);
        else
        {
            start = buildNums.get(0);
            end = buildNums.get(1);
        }

        String first = null;
        String last = null;

        int fields = 0;
        StringBuilder sb;

        for (int i = start; i <= end; i++)
        {
            JenkinsBuild build = JenkinsApi.getBuild(i);
            if(build == null)
                continue;

            String version = build.status == JenkinsBuild.Status.SUCCESS
                    ? build.artifacts.get("JDA").fileNameParts.get(1)
                    : "Build " + build.buildNum + " (failed)";

            if (first == null)
                first = version;
            else
                last = version;

            final List<JenkinsChange> changeSet = build.changes;

            if (changeSet.size() > 0)
            {
                final List<String> changelog = FormattingUtil.getChangelog(changeSet);

                sb = new StringBuilder();

                for (String line : changelog)
                {
                    sb.append(line).append('\n');
                }
                sb.setLength(sb.length() - 1);

                if(sb.length() > MessageEmbed.VALUE_MAX_LENGTH)
                    eb.addField(version, "[`CI Link`](" + build.getUrl() + ") Too many changes to show", false);
                else
                    eb.addField(version, sb.toString(), false);

            }
            else
            {
                eb.addField(version, "No git commits assigned", false);
            }

            if(++fields == 24)
            {
                eb.addField("...", "Max Embed size reached", false);
                break;
            }

        }

        if(first == null)
        {
            channel.sendMessage("Could not find any build in specified range").queue();
            return;
        }

        if (last != null)
            eb.setAuthor("Changelog between builds " + first + " and " + last, JenkinsApi.CHANGE_URL, EmbedUtil.JDA_ICON);
        else
            eb.setAuthor("Changelog for build " + first, JenkinsApi.CHANGE_URL, EmbedUtil.JDA_ICON);

        EmbedUtil.setColor(eb);

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

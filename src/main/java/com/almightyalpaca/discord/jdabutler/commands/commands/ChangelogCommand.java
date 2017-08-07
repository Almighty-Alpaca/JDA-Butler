package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.JDAUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsChange;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChangelogCommand implements Command
{

    private static final String[] ALIASES = new String[]
    { "changes" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event) throws Exception
    {
        final EmbedBuilder eb = new EmbedBuilder();
        final MessageBuilder mb = new MessageBuilder();
        try
        {
            final List<Integer> args = Arrays.stream(content.split("\\s+")).filter(s -> s != null && !s.trim().isEmpty()).map(JDAUtil::getBuildNumber).sorted().collect(Collectors.toList());

            final int start;
            final int end;

            if (args.size() == 0)
                start = end = Bot.config.getInt("jda.version.build");
            else if (args.size() == 1)
                start = end = args.get(0);
            else
            {
                start = args.get(0);
                end = args.get(1);
            }

            final List<JenkinsBuild> responses = new ArrayList<>(end - start + 1);

            //TODO: Move to executor?
            for (int i = start; i <= end; i++)
                responses.add(JenkinsApi.fetchBuild(i));

            String first = null;
            String last = null;

            int fields = 0;

            for (int i = responses.size() - 1; i >= 0; i--)
            {
                JenkinsBuild build;
                try
                {
                    build = responses.get(i);

                    String version = build.status == JenkinsBuild.Status.SUCCESS
                            ? build.artifacts.get(0).fileName.split("[-.]", 3)[1]
                            : Integer.toString(build.buildNum);

                    if (i == 0)
                        first = version;
                    else if (i == responses.size() - 1)
                        last = version;

                    final List<JenkinsChange> changeSet = build.changes;

                    if (changeSet.size() > 0)
                    {

                        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

                        final List<String> changelog = FormattingUtil.getChangelog(changeSet);

                        int currentFields = changelog.size();

                        if (fields + changelog.size() > 24)
                        {
                            currentFields = Math.min(24 - fields, changelog.size());
                            fields = 24;
                        }
                        else
                        {
                            currentFields = changelog.size();
                            fields += currentFields;
                        }

                        for (int j = 0; j < currentFields; j++)
                        {
                            final String field = changelog.get(j);
                            eb.addField(j == 0 ? version : "", field, false);
                        }

                        if (fields == 24)
                        {

                            eb.addField("max embed length reached", "", false);
                            break;
                        }

                    }

                }
                catch (final Exception e)
                {
                    Throwable cause = e;
                    do
                        if (Objects.toString(cause.getMessage()).contains("time") && Objects.toString(cause.getMessage()).contains("out"))
                        {
                            Bot.LOG.fatal("!changelog connection timed out!");
                            break;
                        }
                    while ((cause = cause.getCause()) != null);
                    throw e;
                }
            }

            if (last != null)
                eb.setAuthor("Changelog between builds " + first + " and " + last, JenkinsApi.CHANGE_URL, EmbedUtil.JDA_ICON);
            else
                eb.setAuthor("Changelog for build " + first, JenkinsApi.CHANGE_URL, EmbedUtil.JDA_ICON);

            EmbedUtil.setColor(eb);

        }
        catch (final NumberFormatException e)
        {
            mb.append("Invalid build number!");
        }

        if (!eb.isEmpty())
            mb.setEmbed(eb.build());

        channel.sendMessage(mb.build()).queue();
    }

    @Override
    public String[] getAliases()
    {
        return ChangelogCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return "`!changelog VERSION` - Shows changes for some JDA version";
    }

    @Override
    public String getName()
    {
        return "changelog";
    }
}

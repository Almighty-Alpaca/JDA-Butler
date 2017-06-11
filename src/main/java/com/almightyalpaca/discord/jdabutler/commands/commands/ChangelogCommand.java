package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.JDAUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
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

            final List<Future<HttpResponse<String>>> responses = new ArrayList<>(end - start + 1);

            for (int i = start; i <= end; i++)
                responses.add(Unirest.get(String.format("http://%s:8080/job/JDA/%d/api/json", JDAUtil.JENKINS_BASE.get(), i)).asStringAsync());

            String first = null;
            String last = null;

            int fields = 0;

            for (int i = responses.size() - 1; i >= 0; i--)
            {
                String response = null;
                try
                {
                    response = responses.get(i).get().getBody();
                    final JSONObject object = new JSONObject(response);

                    final JSONArray artifacts = object.getJSONArray("artifacts");

                    final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
                    String version = displayPath.substring(displayPath.indexOf("-") + 1);
                    version = version.substring(0, version.length() - 4);
                    final int index = version.lastIndexOf("-");
                    if (index > 0)
                        version = version.substring(0, index);

                    if (i == 0)
                        first = version;
                    else if (i == responses.size() - 1)
                        last = version;

                    final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

                    if (changeSets.length() > 0)
                    {

                        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

                        final List<String> changelog = FormattingUtil.getChangelog(changeSets);

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
                eb.setAuthor("Changelog between builds " + first + " and " + last, "http://home.dv8tion.net:8080/job/JDA/changes", EmbedUtil.JDA_ICON);
            else
                eb.setAuthor("Changelog for build " + first, "http://home.dv8tion.net:8080/job/JDA/changes", EmbedUtil.JDA_ICON);

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

package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class Changelog implements Command {
    @Override
    public void dispatch(String[] arg, User sender, TextChannel channel, Message message, String content) {
        EmbedBuilder eb = new EmbedBuilder();
        MessageBuilder mb = new MessageBuilder();
        try {
            final List<Integer> args = Arrays.stream(content.split(" ")).filter(s -> s != null && !s.trim().isEmpty()).map(Integer::parseInt).sorted().collect(Collectors.toList());

            final int start;
            final int end;

            if (args.size() == 0) {
                start = end = Bot.config.getInt("jda.version.build");
            } else if (args.size() == 1) {
                start = end = args.get(0);
            } else {
                start = args.get(0);
                end = args.get(1);
            }

            final List<Future<HttpResponse<String>>> responses = new ArrayList<>(end - start + 1);

            for (int i = start; i <= end; i++) {
                responses.add(Unirest.get("http://home.dv8tion.net:8080/job/JDA/" + i + "/api/json").asStringAsync());
            }

            String first = null;
            String last = null;

            for (int i = responses.size() - 1; i >= 0; i--) {
                String response = null;
                try {
                    response = responses.get(i).get().getBody();
                    final JSONObject object = new JSONObject(response);

                    final JSONArray artifacts = object.getJSONArray("artifacts");

                    final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
                    String version = displayPath.substring(displayPath.indexOf("-") + 1);
                    version = version.substring(0, version.length() - 4);
                    final int index = version.lastIndexOf("-");
                    if (index > 0) {
                        version = version.substring(0, index);
                    }

                    if (i == 0) {
                        first = version;
                    } else if (i == responses.size() - 1) {
                        last = version;
                    }

                    final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

                    if (changeSets.length() > 0) {

                        eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE);

                        final List<String> changelog = FormattingUtil.getChangelog(changeSets);

                        for (int j = 0; j < changelog.size(); j++) {
                            final String field = changelog.get(j);
                            eb.addField(j == 0 ? version : "", field, false);

                        }

                    }

                } catch (final Exception e) {
                    Throwable cause = e;
                    do {
                        if (Objects.toString(cause.getMessage()).contains("time") && Objects.toString(cause.getMessage()).contains("out")) {
                            Bot.LOG.fatal("!changelog connection timed out!");
                            return;
                        }
                    } while ((cause = e.getCause()) != null);
                    Bot.LOG.fatal("The following response errored: " + response);
                    Bot.LOG.log(e);
                }
            }

            if (last != null) {
                eb.setAuthor("Changelog between builds " + first + " and " + last, "http://home.dv8tion.net:8080/job/JDA/changes", EmbedUtil.JDA_ICON);
            } else {
                eb.setAuthor("Changelog for build " + first, "http://home.dv8tion.net:8080/job/JDA/changes", EmbedUtil.JDA_ICON);
            }

            EmbedUtil.setColor(eb);

        } catch (final NumberFormatException e) {
            mb.append("Invalid build number!");
        }
        channel.sendMessage(mb.setEmbed(eb.build()).build()).queue();
    }

    @Override
    public String getName() {
        return "changelog";
    }

    @Override
    public String getHelp() {
        return "`!changelog VERSION` - Shows changes for some JDA version";
    }
}

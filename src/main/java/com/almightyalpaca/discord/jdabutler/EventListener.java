package com.almightyalpaca.discord.jdabutler;

import com.almightyalpaca.discord.jdabutler.eval.Engine;
import com.kantenkugel.discordbot.moduleutils.DocParser;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EventListener extends ListenerAdapter {

	static ScheduledExecutorService	executor	= new ScheduledThreadPoolExecutor(1, (r) -> {
													final Thread t = new Thread(r);
													t.setDaemon(true);
													t.setUncaughtExceptionHandler((final Thread thread, final Throwable throwable) -> {
																									throwable.printStackTrace();
																								});
													t.setPriority(Thread.NORM_PRIORITY);
													return t;
												});

	private static boolean			started;

	public static void start() {

		if (!EventListener.started) {
			EventListener.started = true;

			EventListener.executor.scheduleAtFixedRate(() -> {

				String response = null;

				// JDA
				try {
					Bot.LOG.debug("Checking for JDA 3 updates...");
					response = Unirest.get(String.format("http://%s:8080/job/JDA/lastBuild/api/json", JDAUtil.JENKINS_BASE.get())).asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda.version.build", -1)) {
						Bot.LOG.debug("Update found!");

						final JSONArray artifacts = object.getJSONArray("artifacts");

						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
						String version = displayPath.substring(displayPath.indexOf("-") + 1);
						version = version.substring(0, version.length() - 4);
						final int index = version.lastIndexOf("-");
						if (index > 0) {
							version = version.substring(0, index);
						}

						Bot.config.put("jda.version.build", build);
						Bot.config.put("jda.version.name", version);

						Bot.config.save();

						EventListener.executor.submit(() -> {
							DocParser.reFetch();
							GradleProjectDropboxUploader.uploadProject();
						});

						if (Bot.config.getBoolean("testing", true)) {
							Bot.LOG.debug("Skipping announcement!");
							return;
						}

						final String timestamp = FormattingUtil.formatTimestap(object.getLong("timestamp"));

						final EmbedBuilder eb = new EmbedBuilder();

						final MessageBuilder mb = new MessageBuilder();

						final JSONArray culprits = object.getJSONArray("culprits");

						FormattingUtil.setFooter(eb, culprits, timestamp);

						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

						mb.append(Bot.getRoleJdaUpdates());

						eb.setAuthor("JDA 3 version " + version + " has been released\n", "http://home.dv8tion.net:8080/job/JDA/" + build, EmbedUtil.JDA_ICON);

						EmbedUtil.setColor(eb);

						if (changeSets.length() > 0) {

							eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);

							final List<String> changelog = FormattingUtil.getChangelog(changeSets);

							int fields;

							if (changelog.size() > 25) {
								fields = 24;
							} else {
								fields = Math.min(changelog.size(), 25);
							}

							for (int j = 0; j < fields; j++) {
								final String field = changelog.get(j);
								eb.addField(j == 0 ? "Commits:" : "", field, false);
							}

							if (changelog.size() > 25) {
								eb.addField("", "max embed length reached", false);
							}

						}

						final MessageEmbed embed = eb.build();

						mb.setEmbed(embed);

						mb.build();

						final Role role = Bot.getRoleJdaUpdates();
						final TextChannel channel = Bot.getChannelAnnouncements();

						role.getManager().setMentionable(true).queue(s -> channel.sendMessage(mb.build()).queue(m -> role.getManager().setMentionable(false).queue()));

					}
				} catch (final Exception e) {
					Throwable cause = e;
					do {
						if (Objects.toString(cause.getMessage()).contains("time") && Objects.toString(cause.getMessage()).contains("out")) {
							Bot.LOG.fatal("JDA 3 update checker connection timed out!");
							return;
						}
					} while ((cause = e.getCause()) != null);
					Bot.LOG.fatal("The following response errored: " + response);
					Bot.LOG.log(e);
				}

			}, 0, 30, TimeUnit.SECONDS);
		}
	}

	@Override
	public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
		final Guild guild = event.getGuild();
		if (guild.getId().equals("125227483518861312")) {
			final Member member = event.getMember();
			final User user = member.getUser();
			Role role;
			if (user.isBot()) {
				role = Bot.getRoleBots();
			} else {
				role = Bot.getRoleJdaFanclub();
			}

			final AuditableRestAction<Void> action = guild.getController().addRolesToMember(member, role).reason("Auto Role");
			final String message = String.format("Added %#s (%d) to %s", user, user.getIdLong(), role.getName());
			action.queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, message), Bot.LOG::log);
		}
	}

	@Override
	public void onShutdown(final ShutdownEvent event) {
		EventListener.executor.shutdown();
		Engine.shutdown();
	}
}

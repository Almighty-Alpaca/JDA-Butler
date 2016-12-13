package com.almightyalpaca.discord.jdabutler;

import java.lang.management.ManagementFactory;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;

import com.kantenkugel.discordbot.moduleutils.DocParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import com.almightyalpaca.discord.jdabutler.util.StringUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.SimpleLog;

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
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda.version.build", -1)) {
						Bot.LOG.debug("Update found!");

						final String timestamp = FormattingUtil.formatTimestap(object.getLong("timestamp"));

						final EmbedBuilder eb = new EmbedBuilder();

						final MessageBuilder mb = new MessageBuilder();

						final JSONArray culprits = object.getJSONArray("culprits");

						FormattingUtil.setFooter(eb, culprits, timestamp);

						final JSONArray artifacts = object.getJSONArray("artifacts");

						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
						String version = displayPath.substring(displayPath.indexOf("-") + 1);
						version = version.substring(0, version.length() - 4);
						final int index = version.lastIndexOf("-");
						if (index > 0) {
							version = version.substring(0, index);
						}

						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

						mb.append(Bot.getRoleJdaUpdates());

						eb.setAuthor("JDA 3 build " + version + " has been released\n", "http://home.dv8tion.net:8080/job/JDA/" + build, EmbedUtil.JDA_ICON);

						EmbedUtil.setColor(eb);

						if (changeSets.length() > 0) {

							eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE);

							eb.addField("Commits:", FormattingUtil.getChangelog(changeSets), true);
						}

						final MessageEmbed embed = eb.build();

						mb.setEmbed(embed);

						final Message message = mb.build();

						Bot.getRoleJdaUpdates().getManager().setMentionable(true).block();

						Bot.getChannelAnnouncements().sendMessage(message).block();

						Bot.getRoleJdaUpdates().getManager().setMentionable(false).queue();

						Bot.config.put("jda.version.build", build);
						Bot.config.put("jda.version.name", version);

						Bot.config.save();
						DocParser.reFetch();

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

			guild.getController().addRolesToMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId()
					+ ") to " + role.getName()), t -> Bot.LOG.log(t));

		}
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		final Guild guild = event.getGuild();
		final TextChannel channel = event.getChannel();
		if (guild.getId().equals("81384788765712384") && !channel.getId().equals("129750718931271681")) {
			return;
		}

		final Message msg = event.getMessage();
		String text = msg.getRawContent();
		final User user = event.getAuthor();

		final MessageBuilder mb = new MessageBuilder();
		final EmbedBuilder eb = new EmbedBuilder();

		if (text.startsWith("!version") || text.startsWith("!latest")) {

			EmbedUtil.setColor(eb);

			eb.setAuthor("Latest versions", null, EmbedUtil.JDA_ICON);
			eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE);

			eb.addField("JDA", "[" + Bot.config.getString("jda.version.name") + "](http://home.dv8tion.net:8080/job/JDA/lastSuccessfulBuild/)", true);
			eb.addField("Lavaplayer", "[" + Lavaplayer.getLatestVersion() + "](https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord)", true);

		} else if (text.startsWith("!shutdown")) {
			if (Bot.isAdmin(user)) {
				Bot.shutdown();
			}
		} else if (text.startsWith("!docs ")) {
			text = text.substring(6);
			mb.append(DocParser.get(text));
		} else if (text.startsWith("!gradle")) {
			text = text.substring(7);

			final boolean lavaplayer = text.contains("player");
			final boolean pretty = text.contains("pretty");

			String author = "Gradle dependencies for JDA";
			if (lavaplayer) {
				author += " and Lavaplayer";
			}

			eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

			String field = "If you don't know gradle type `!build.gradle` for a complete gradle build file\n\n```gradle\n";

			final Collection<Pair<String, String>> repositories = new ArrayList<>(2);
			final Collection<Triple<String, String, String>> dependencies = new ArrayList<>(2);

			dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
			repositories.add(new ImmutablePair<String, String>("jcenter()", null));

			if (lavaplayer) {
				dependencies.add(new ImmutableTriple<>(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion()));
				repositories.add(new ImmutablePair<>(Lavaplayer.REPO_NAME, Lavaplayer.REPO_URL));
			}

			field += GradleUtil.getDependencyBlock(dependencies, pretty) + "\n";
			field += "\n";

			field += GradleUtil.getRepositoryBlock(repositories) + "\n";

			field += "```";

			eb.addField("", field, false);

		} else if (text.startsWith("!maven")) {

			final boolean lavaplayer = text.contains("player");

			String author = "Maven dependencies for JDA";
			if (lavaplayer) {
				author += " and Lavaplayer";
			}

			eb.setAuthor(author, null, EmbedUtil.JDA_ICON);

			String field = "If you don't know maven type `!pom.xml` for a complete maven build file \n\n```xml\n";

			field += MavenUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda.version.name"), null) + "\n";
			if (lavaplayer) {
				field += MavenUtil.getDependencyString(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion(), null) + "\n";
			}

			field += "\n";

			field += MavenUtil.getRepositoryString("jcenter", "jcenter-bintray", "http://jcenter.bintray.com", null) + "\n";

			if (lavaplayer) {
				field += MavenUtil.getRepositoryString("sedmelluq", "sedmelluq", "http://maven.sedmelluq.com/", null) + "\n";
			}

			field += "```";

			eb.addField("", field, false);
		} else if (text.startsWith("!jar")) {
			final String version = Bot.config.getString("jda.version.name");
			final String build = Bot.config.getString("jda.version.build");
			mb.append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-javadoc.jar").append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build
					+ "/artifact/build/libs/JDA-" + version + "-sources.jar").append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + ".jar")
					.append("\n").append("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-withDependencies-" + version + ".jar");

		} else if (text.startsWith("!build.gradle")) {
			text = text.substring(13);

			final boolean lavaplayer = true;
			final boolean pretty = true;

			final Collection<Pair<String, String>> repositories = new ArrayList<>(2);
			final Collection<Triple<String, String, String>> dependencies = new ArrayList<>(2);

			dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
			repositories.add(new ImmutablePair<String, String>("jcenter()", null));

			if (lavaplayer) {
				dependencies.add(new ImmutableTriple<>(Lavaplayer.GROUP_ID, Lavaplayer.ARTIFACT_ID, Lavaplayer.getLatestVersion()));
				repositories.add(new ImmutablePair<>(Lavaplayer.REPO_NAME, Lavaplayer.REPO_URL));
			}

			mb.appendCodeBlock(GradleUtil.getBuildFile(dependencies, repositories, pretty), "gradle");
		} else if (text.startsWith("!notify")) {
			text = text.substring(7);
			final Member member = event.getMember();

			if (text.contains("all") || text.contains("both")) {
				final List<Role> roles = new ArrayList<>(3);
				roles.add(Bot.getRoleJdaUpdates());
				roles.add(Bot.getRoleLavaplayerUpdates());
				roles.removeAll(member.getRoles());

				if (roles.size() == 0) {
					guild.getController().removeRolesFromMember(member, Bot.getRoleJdaUpdates(), Bot.getRoleLavaplayerUpdates()).queue(v -> {
						Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId() + ") from " + Bot.getRoleJdaUpdates().getName());
						Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId() + ") from " + Bot.getRoleLavaplayerUpdates().getName());
					}, t -> Bot.LOG.log(t));
				} else {
					guild.getController().addRolesToMember(member, roles).queue(v -> roles.forEach(role -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user
							.getDiscriminator() + " (" + user.getId() + ") to " + role.getName())), t -> Bot.LOG.log(t));
				}

			} else {
				final Role role;

				if (text.contains("player")) {
					role = Bot.getRoleLavaplayerUpdates();
				} else {
					role = Bot.getRoleJdaUpdates();
				}

				if (member.getRoles().contains(role)) {
					guild.getController().removeRolesFromMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user
							.getId() + ") from " + role.getName()), t -> Bot.LOG.log(t));
				} else {
					guild.getController().addRolesToMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId()
							+ ") to " + role.getName()), t -> Bot.LOG.log(t));
				}
			}

			msg.addReaction("\uD83D\uDC4D").queue();

		} else if (text.startsWith("!ping")) {
			event.getChannel().sendMessage("Ping: ...").queue(m -> m.editMessage("Ping: " + event.getMessage().getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS) + "ms").queue());
		} else if (text.startsWith("!uptime")) {

			final long duration = ManagementFactory.getRuntimeMXBean().getUptime();

			final long years = duration / 31104000000L;
			final long months = duration / 2592000000L % 12;
			final long days = duration / 86400000L % 30;
			final long hours = duration / 3600000L % 24;
			final long minutes = duration / 60000L % 60;
			final long seconds = duration / 1000L % 60;

			String uptime = "";
			uptime += years == 0 ? "" : years + " Year" + (years > 1 ? "s" : "") + ", ";
			uptime += months == 0 ? "" : months + " Month" + (months > 1 ? "s" : "") + ", ";
			uptime += days == 0 ? "" : days + " Day" + (days > 1 ? "s" : "") + ", ";
			uptime += hours == 0 ? "" : hours + " Hour" + (hours > 1 ? "s" : "") + ", ";
			uptime += minutes == 0 ? "" : minutes + " Minute" + (minutes > 1 ? "s" : "") + ", ";
			uptime += seconds == 0 ? "" : seconds + " Second" + (seconds > 1 ? "s" : "") + ", ";

			uptime = StringUtils.replaceLast(uptime, ", ", "");
			uptime = StringUtils.replaceLast(uptime, ",", " and");

			mb.append(uptime);
		} else if (text.startsWith("!changelog")) {
			text = text.substring(10);

			final String[] args = text.split("\\s+");

			final int start;
			final int end;

			try {
				if (args.length == 0) {
					start = end = Integer.parseInt(Bot.config.getString("jda.version.build"));
				} else if (args.length == 1) {
					start = end = Integer.parseInt(args[0]);
				} else {
					start = Integer.parseInt(args[0]);
					end = Integer.parseInt(args[1]);
				}

				final List<Future<HttpResponse<String>>> responses = new ArrayList<>(end - start + 1);

				System.out.println(end - start + 1);

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

							eb.addField(version, FormattingUtil.getChangelog(changeSets), false);

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

				eb.setAuthor("Changelog between builds " + first + " and " + last, "http://home.dv8tion.net:8080/job/JDA/changes", EmbedUtil.JDA_ICON);

				EmbedUtil.setColor(eb);

			} catch (final NumberFormatException e) {
				mb.append("Invalid build number!");
			}

		}

		if (!eb.isEmpty()) {
			mb.setEmbed(eb.build());
		}

		if (!mb.isEmpty()) {
			event.getChannel().sendMessage(mb.build()).queue();
		}

	}
}

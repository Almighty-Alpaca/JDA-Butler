package com.almightyalpaca.discord.jdabutler;

import java.lang.management.ManagementFactory;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONArray;
import org.json.JSONObject;

import com.kantenkugel.discordbot.moduleutils.DocParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
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
				// Old bintray fetcher
				// try {
				// final HttpResponse<String> response = Unirest.get("https://dl.bintray.com/dv8fromtheworld/maven/net/dv8tion/JDA/maven-metadata.xml").asString();
				// final JSONObject object = XML.toJSONObject(response.getBody());
				// final JSONObject versioning = object.getJSONObject("metadata").getJSONObject("versioning");
				// final String version = versioning.getString("release");
				// if (!version.equals(Bot.config.getString("jda.version", "none"))) {
				// Bot.config.put("jda.version", version);
				// Bot.config.save();
				// final MessageBuilder builder = new MessageBuilder();
				// builder.appendString("JDA build ").appendString(version, Formatting.BOLD).appendString(" has been released!\n");
				// builder.appendString("<http://home.dv8tion.net:8080/job/JDA/" + version.substring(version.lastIndexOf("_") + 1) + ">").appendString("\n");
				// builder.appendString("<https://bintray.com/dv8fromtheworld/maven/JDA/" + version + ">").appendString("\n");
				// Bot.getChannelAnnouncements().sendMessage(builder.build());
				// }
				// } catch (final Exception e) {
				// e.printStackTrace();
				// }

				String response = null;
				boolean update = false;

				// JDA
				try {
					Bot.LOG.debug("Checking for JDA 3 updates...");
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda.version.build", -1)) {
						Bot.LOG.debug("Update found!");
						update = true;

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

						mb.appendMention(Bot.getRoleJdaUpdates());

						eb.setAuthor("JDA 3 build " + version + " has been released\n", "http://home.dv8tion.net:8080/job/JDA/" + build,
								"https://cdn.discordapp.com/icons/125227483518861312/c9ea3e5510039dd487171c300a363813.jpg");

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

				// JDA-Player
				try {
					Bot.LOG.debug("Checking for JDA-Player updates...");
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA-Player/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda-player.version.build", -1)) {
						Bot.LOG.debug("Update found!");
						update = true;
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

						mb.appendMention(Bot.getRoleJdaUpdates());

						eb.setAuthor("JDA-Player build " + version + " has been released\n", "http://home.dv8tion.net:8080/job/JDA/" + build,
								"https://cdn.discordapp.com/icons/125227483518861312/c9ea3e5510039dd487171c300a363813.jpg");

						EmbedUtil.setColor(eb);

						if (changeSets.length() > 0) {

							eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE);

							eb.addField("Commits:", FormattingUtil.getChangelog(changeSets), true);
						}

						final MessageEmbed embed = eb.build();

						mb.setEmbed(embed);

						final Message message = mb.build();

						Bot.getRoleJdaPlayerUpdates().getManager().setMentionable(true).block();

						Bot.getChannelAnnouncements().sendMessage(message).block();

						Bot.getRoleJdaPlayerUpdates().getManager().setMentionable(false).queue();

						Bot.config.put("jda-player.version.build", build);
						Bot.config.put("jda-player.version.name", version);

						Bot.config.save();
					}
				} catch (final Exception e) {
					Throwable cause = e;
					do {
						if (Objects.toString(cause.getMessage()).contains("time") && Objects.toString(cause.getMessage()).contains("out")) {
							Bot.LOG.fatal("JDA-Player update checker connection timed out!");
							return;
						}
					} while ((cause = e.getCause()) != null);
					Bot.LOG.fatal("The following response errored: " + response);
					Bot.LOG.log(e);
				}

				//				// JDA 2 Legacy
				//				try {
				//					Bot.LOG.debug("Checking for JDA updates...");
				//					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA/lastBuild/api/json").asString().getBody();
				//					final JSONObject object = new JSONObject(response);
				//					final int build = Integer.valueOf(object.getString("id"));
				//					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda2.version.build", -1)) {
				//						Bot.LOG.debug("Update found!");
				//						update = true;
				//
				//						MessageBuilder builder = new MessageBuilder();
				//
				//						final JSONArray artifacts = object.getJSONArray("artifacts");
				//
				//						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
				//						final String version = displayPath.substring(0, displayPath.lastIndexOf("-")).substring(4);
				//
				//						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");
				//
				//						builder.appendString("**JDA** build ").appendString(version, Formatting.BOLD).appendString(" has been released!   ").appendMention(Bot.getRoleJdaUpdates()).appendString(
				//								"\n\n");
				//
				//						if (changeSets.length() > 0) {
				//							builder.appendString("Commits:", Formatting.BOLD).appendString("\n");
				//							for (int i = 0; i < changeSets.length(); i++) {
				//								final JSONObject item = changeSets.getJSONObject(i);
				//								final String id = item.getString("id").substring(0, 6);
				//								final String comment = item.getString("comment");
				//								final String[] lines = comment.split("\n");
				//								for (int j = 0; j < lines.length; j++) {
				//									if (builder.getLength() > 1800) {
				//										Bot.getRoleJdaUpdates().getManager().setMentionable(true).block();
				//										Bot.getChannelAnnouncements().sendMessage(builder.build()).block();
				//										Bot.getRoleJdaUpdates().getManager().setMentionable(false).queue();
				//										builder = new MessageBuilder();
				//									}
				//									builder.appendString(j == 0 ? id : "......", Formatting.BLOCK).appendString(" ").appendString(lines[j]).appendString("\n");
				//								}
				//							}
				//						}
				//
				//						//						builder.appendString("\n");
				//						//						builder.appendString("Downloads:", Formatting.BOLD).appendString("\n");
				//						//
				//						//						builder.appendString("http://home.dv8tion.net:8080/job/JDA/").appendString(String.valueOf(build)).appendString("/artifact/build/libs/ \n");
				//						//
				//						//						for (int i = 0; i < artifacts.length(); i++) {
				//						//							final JSONObject artifact = artifacts.getJSONObject(i);
				//						//							final String path = artifact.getString("relativePath");
				//						//							builder.appendString("<").appendString("http://home.dv8tion.net:8080/job/JDA/").appendString(String.valueOf(build)).appendString("/artifact/").appendString(path).appendString(
				//						//									">\n");
				//						//						}
				//						//
				//						//						builder.appendString("\n");
				//						//
				//						//						builder.appendString("Gradle:", Formatting.BOLD).appendString("\n");
				//						//
				//						//						builder.appendCodeBlock("compile 'net.dv8tion:JDA:" + version + "'", "gradle").appendString("\n");
				//						//
				//						//						builder.appendString("Maven:", Formatting.BOLD).appendString("\n");
				//						//
				//						//						builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>JDA</artifactId>\n    <version>" + version + "</version>\n</dependency>\n", "html")
				//						//								.appendString("\n");
				//
				//						final Message message = builder.build();
				//
				//						Bot.getRoleJdaUpdates().getManager().setMentionable(true).block();
				//
				//						Bot.getChannelAnnouncements().sendMessage(message).block();
				//
				//						Bot.getRoleJdaUpdates().getManager().setMentionable(false).queue();
				//
				//						Bot.config.put("jda2.version.build", build);
				//						Bot.config.put("jda2.version.name", version);
				//
				//						//						final int lastReleaseBuild = Bot.config.getInt("jda2.version.release.build", -1);
				//						//
				//						//						final JSONArray actions = object.getJSONArray("actions");
				//						//
				//						//						JSONObject master = null;
				//						//
				//						//						for (int i = 0; i < actions.length(); i++) {
				//						//							final JSONObject action = actions.getJSONObject(i);
				//						//							if (action.has("buildsByBranchName")) {
				//						//								master = action.getJSONObject("buildsByBranchName").getJSONObject("origin/master");
				//						//								break;
				//						//							}
				//						//						}
				//						//
				//						//						if (master != null) {
				//						//							final int currentReleaseBuild = master.getInt("buildNumber");
				//						//							if (currentReleaseBuild != lastReleaseBuild) {
				//						//								final JSONObject releaseResponse = Unirest.get("http://home.dv8tion.net:8080/job/JDA/" + currentReleaseBuild + "/api/json").asJson().getBody().getObject();
				//						//
				//						//								final JSONArray releaseArtifacts = releaseResponse.getJSONArray("artifacts");
				//						//
				//						//								final String releaseDisplayPath = releaseArtifacts.getJSONObject(0).getString("displayPath");
				//						//								final String releaseVersion = releaseDisplayPath.substring(0, releaseDisplayPath.lastIndexOf("-")).substring(4);
				//						//
				//						//								Bot.config.put("jda2.version.release.build", lastReleaseBuild);
				//						//								Bot.config.put("jda2.version.release.name", releaseVersion);
				//						//
				//						//							}
				//						//						}
				//
				//					}
				//				} catch (final Exception e) {
				//					if (e.getCause() instanceof ConnectTimeoutException || e.getCause() instanceof SocketTimeoutException) {
				//						Bot.LOG.fatal("JDA update checker connection timed out!");
				//						return;
				//					} else {
				//						Bot.LOG.fatal("The following response errored: " + response);
				//						Bot.LOG.log(e);
				//					}
				//				}

				if (update) {
					Bot.config.save();
					DocParser.reFetch();
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
		final MessageBuilder builder = new MessageBuilder();

		boolean embedPresent = false;

		if (text.startsWith("!version")) {
			builder.appendString("Latest **JDA** version is ").appendString(Bot.config.getString("jda.version.name"), Formatting.BOLD).appendString("\n");
			builder.appendString("Latest **JDA-Player** version is ").appendString(Bot.config.getString("jda-player.version.name"), Formatting.BOLD).appendString("\n");
			builder.appendString("Latest **JDA 2 Legacy** version is ").appendString(Bot.config.getString("jda2.version.name"), Formatting.BOLD);
		} else if (text.startsWith("!shutdown")) {
			final Member member = Bot.getGuildJda().getMember(user);
			if (member != null && member.getRoles().contains(Bot.getRoleStaff())) {
				Bot.shutdown();
			}
		} else if (text.startsWith("!docs ")) {
			text = text.substring(6);
			builder.appendString(DocParser.get(text));
		} else if (text.startsWith("!gradle")) {
			text = text.substring(7);

			final boolean pretty = text.contains("pretty");

			if (text.contains("player")) {
				GradleUtil.getDependencyString("net.dv8tion", "jda-player", Bot.config.getString("jda-player.version.name"), pretty);
			} else if (text.contains("2") || text.contains("legacy")) {
				GradleUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda2.version.name"), pretty);
			} else {
				GradleUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda.version.name"), pretty);
			}

		} else if (text.startsWith("!maven")) {
			text = text.substring(6);

			if (text.contains("player")) {
				builder.appendCodeBlock(MavenUtil.getDependencyString("net.dv8tion", "jda-player", Bot.config.getString("jda-player.version.name"), null), "html").appendString("\n");
			} else if (text.contains("3")) {
				builder.appendCodeBlock(MavenUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda2.version.name"), null), "html").appendString("\n");
			} else {
				builder.appendCodeBlock(MavenUtil.getDependencyString("net.dv8tion", "JDA", Bot.config.getString("jda.version.name"), null), "html").appendString("\n");
			}

		} else if (text.startsWith("!jar")) {
			text = text.substring(4);

			if (text.contains("player")) {
				final String version = Bot.config.getString("jda-player.version.name");
				final String build = Bot.config.getString("jda-player.version.build");
				builder.appendString("http://home.dv8tion.net:8080/job/JDA-Player/" + build + "/artifact/JDA/build/libs/jda-player-" + version + "-javadoc.jar").appendString("\n").appendString(
						"http://home.dv8tion.net:8080/job/JDA-Player/" + build + "/artifact/JDA/build/libs/jda-player-" + version + "-sources.jar").appendString("\n").appendString(
								"http://home.dv8tion.net:8080/job/JDA-Player/" + build + "/artifact/JDA/build/libs/jda-player-" + version + ".jar").appendString("\n").appendString(
										"http://home.dv8tion.net:8080/job/JDA-Player/" + build + "/artifact/JDA/build/libs/jda-player-" + version + "-withDependencies.jar");
			} else if (text.contains("2") || text.contains("legacy")) {
				final String version = Bot.config.getString("jda2.version.name");
				final String build = Bot.config.getString("jda2.version.build");
				builder.appendString("http://home.dv8tion.net:8080/job/JDA%20Legacy/" + build + "/artifact/build/libs/JDA-" + version + "-javadoc.jar").appendString("\n").appendString(
						"http://home.dv8tion.net:8080/job/JDA%20Legacy/" + build + "/artifact/build/libs/JDA-" + version + "-sources.jar").appendString("\n").appendString(
								"http://home.dv8tion.net:8080/job/JDA%20Legacy/" + build + "/artifact/build/libs/JDA-" + version + ".jar").appendString("\n").appendString(
										"http://home.dv8tion.net:8080/job/JDA%20Legacy/" + build + "/artifact/build/libs/JDA-withDependencies-" + version + ".jar");
			} else {
				final String version = Bot.config.getString("jda.version.name");
				final String build = Bot.config.getString("jda.version.build");
				builder.appendString("http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-javadoc.jar").appendString("\n").appendString(
						"http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + "-sources.jar").appendString("\n").appendString(
								"http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-" + version + ".jar").appendString("\n").appendString(
										"http://home.dv8tion.net:8080/job/JDA/" + build + "/artifact/build/libs/JDA-withDependencies-" + version + ".jar");
			}

		} else if (text.startsWith("!build.gradle")) {
			text = text.substring(13);

			final boolean pretty = text.contains("pretty");
			final List<Triple<String, String, String>> dependencies = new ArrayList<>(2);

			if (text.contains("player")) {
				dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda2.version.name")));
				dependencies.add(new ImmutableTriple<>("net.dv8tion", "jda-player", Bot.config.getString("jda-player.version.name")));
			} else if (text.contains("2") || text.contains("legacy")) {
				dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda2.version.name")));
			} else {
				dependencies.add(new ImmutableTriple<>("net.dv8tion", "JDA", Bot.config.getString("jda.version.name")));
			}
			builder.appendCodeBlock(GradleUtil.getBuildFile(dependencies, pretty), "gradle");
		} else if (text.startsWith("!notify")) {
			text = text.substring(7);
			final Member member = event.getMember();

			if (text.contains("all") || text.contains("both")) {
				final List<Role> roles = new ArrayList<>(3);
				roles.add(Bot.getRoleJdaUpdates());
				roles.add(Bot.getRoleJdaPlayerUpdates());
				roles.removeAll(member.getRoles());

				if (roles.size() == 0) {
					guild.getController().removeRolesFromMember(member, Bot.getRoleJdaUpdates(), Bot.getRoleJdaPlayerUpdates()).queue(v -> {
						Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId() + ") from " + Bot.getRoleJdaUpdates().getName());
						Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId() + ") from " + Bot.getRoleJdaPlayerUpdates().getName());
					}, t -> Bot.LOG.log(t));
				} else {
					guild.getController().addRolesToMember(member, roles).queue(v -> roles.forEach(role -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user
							.getDiscriminator() + " (" + user.getId() + ") to " + role.getName())), t -> Bot.LOG.log(t));
				}

			} else {
				final Role role;

				if (text.contains("player")) {
					role = Bot.getRoleJdaPlayerUpdates();
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

			uptime = com.almightyalpaca.discord.jdabutler.util.StringUtils.replaceLast(uptime, ", ", "");
			uptime = com.almightyalpaca.discord.jdabutler.util.StringUtils.replaceLast(uptime, ",", " and");

			builder.appendString(uptime);
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

				final EmbedBuilder eb = new EmbedBuilder();

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

				eb.setAuthor("Changelog between builds " + first + " and " + last, "http://home.dv8tion.net:8080/job/JDA/changes",
						"https://cdn.discordapp.com/icons/125227483518861312/c9ea3e5510039dd487171c300a363813.jpg");

				EmbedUtil.setColor(eb);

				final MessageEmbed embed = eb.build();

				builder.setEmbed(embed);
				embedPresent = true;
			} catch (final NumberFormatException e) {
				builder.appendString("Invalid build number!");
			}

		}

		if (builder.getLength() > 0 || embedPresent) {
			event.getChannel().sendMessage(builder.build()).queue();
		}

	}
}

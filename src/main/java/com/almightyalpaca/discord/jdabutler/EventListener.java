package com.almightyalpaca.discord.jdabutler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.kantenkugel.discordbot.moduleutils.DocParser;
import com.mashape.unirest.http.Unirest;

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
				// Bot.CHANNEL_ANNOUNCEMENTS.sendMessage(builder.build());
				// }
				// } catch (final Exception e) {
				// e.printStackTrace();
				// }

				// JDA
				String response = null;
				boolean update = false;
				try {
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda.version.build", -1)) {
						update = true;

						final MessageBuilder builder = new MessageBuilder();

						final JSONArray artifacts = object.getJSONArray("artifacts");

						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
						final String version = displayPath.substring(0, displayPath.lastIndexOf("-")).substring(4);

						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

						builder.appendString("**JDA** build ").appendString(version, Formatting.BOLD).appendString(" has been released!   ").appendMention(Bot.ROLE_JDA_UPDATES).appendString("\n\n");

						if (changeSets.length() > 0) {
							builder.appendString("Commits:", Formatting.BOLD).appendString("\n");
							for (int i = 0; i < changeSets.length(); i++) {
								final JSONObject item = changeSets.getJSONObject(i);
								final String id = item.getString("id").substring(0, 6);
								final String comment = item.getString("comment");
								final String[] lines = comment.split("\n");
								for (int j = 0; j < lines.length; j++) {
									builder.appendString(j == 0 ? id : "......", Formatting.BLOCK).appendString(" ").appendString(lines[j]).appendString("\n");
								}
							}
						}

						//						builder.appendString("\n");
						//						builder.appendString("Downloads:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendString("http://home.dv8tion.net:8080/job/JDA/").appendString(String.valueOf(build)).appendString("/artifact/build/libs/ \n");
						//
						//						for (int i = 0; i < artifacts.length(); i++) {
						//							final JSONObject artifact = artifacts.getJSONObject(i);
						//							final String path = artifact.getString("relativePath");
						//							builder.appendString("<").appendString("http://home.dv8tion.net:8080/job/JDA/").appendString(String.valueOf(build)).appendString("/artifact/").appendString(path).appendString(
						//									">\n");
						//						}
						//
						//						builder.appendString("\n");
						//
						//						builder.appendString("Gradle:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendCodeBlock("compile 'net.dv8tion:JDA:" + version + "'", "gradle").appendString("\n");
						//
						//						builder.appendString("Maven:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>JDA</artifactId>\n    <version>" + version + "</version>\n</dependency>\n", "html")
						//								.appendString("\n");

						final Message message = builder.build();

						Bot.ROLE_JDA_UPDATES.getManager().setMentionable(true).block();

						Bot.CHANNEL_ANNOUNCEMENTS.sendMessage(message).block();

						Bot.ROLE_JDA_UPDATES.getManager().setMentionable(false).queue();

						Bot.config.put("jda.version.build", build);
						Bot.config.put("jda.version.name", version);

						//						final int lastReleaseBuild = Bot.config.getInt("jda.version.release.build", -1);
						//
						//						final JSONArray actions = object.getJSONArray("actions");
						//
						//						JSONObject master = null;
						//
						//						for (int i = 0; i < actions.length(); i++) {
						//							final JSONObject action = actions.getJSONObject(i);
						//							if (action.has("buildsByBranchName")) {
						//								master = action.getJSONObject("buildsByBranchName").getJSONObject("origin/master");
						//								break;
						//							}
						//						}
						//
						//						if (master != null) {
						//							final int currentReleaseBuild = master.getInt("buildNumber");
						//							if (currentReleaseBuild != lastReleaseBuild) {
						//								final JSONObject releaseResponse = Unirest.get("http://home.dv8tion.net:8080/job/JDA/" + currentReleaseBuild + "/api/json").asJson().getBody().getObject();
						//
						//								final JSONArray releaseArtifacts = releaseResponse.getJSONArray("artifacts");
						//
						//								final String releaseDisplayPath = releaseArtifacts.getJSONObject(0).getString("displayPath");
						//								final String releaseVersion = releaseDisplayPath.substring(0, releaseDisplayPath.lastIndexOf("-")).substring(4);
						//
						//								Bot.config.put("jda.version.release.build", lastReleaseBuild);
						//								Bot.config.put("jda.version.release.name", releaseVersion);
						//
						//							}
						//						}

					}
				} catch (final Exception e) {
					Bot.LOG.fatal("The following response errored: " + response);
					Bot.LOG.log(e);
				}

				// JDA-Player
				try {
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA-Player/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda-player.version.build", -1)) {
						update = true;
						final MessageBuilder builder = new MessageBuilder();

						final JSONArray artifacts = object.getJSONArray("artifacts");

						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
						final String version = displayPath.substring(0, displayPath.lastIndexOf("-")).substring(11);

						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

						builder.appendString("**JDA-Player** build ").appendString(version, Formatting.BOLD).appendString(" has been released!   ").appendMention(Bot.ROLE_JDA_PLAYER_UPDATES)
								.appendString("\n\n");

						if (changeSets.length() > 0) {
							builder.appendString("Commits:", Formatting.BOLD).appendString("\n");
							for (int i = 0; i < changeSets.length(); i++) {
								final JSONObject item = changeSets.getJSONObject(i);
								final String id = item.getString("id").substring(0, 6);
								final String comment = item.getString("comment");
								final String[] lines = comment.split("\n");
								for (int j = 0; j < lines.length; j++) {
									builder.appendString(j == 0 ? id : "......", Formatting.BLOCK).appendString(" ").appendString(lines[j]).appendString("\n");
								}
							}
						}
						//						builder.appendString("\n");
						//						builder.appendString("Downloads:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendString("http://home.dv8tion.net:8080/job/JDA-Player/").appendString(String.valueOf(build)).appendString("/artifact/JDA/build/libs/ \n");
						//
						//						for (int i = 0; i < artifacts.length(); i++) {
						//							final JSONObject artifact = artifacts.getJSONObject(i);
						//							final String path = artifact.getString("relativePath");
						//							builder.appendString("<").appendString("http://home.dv8tion.net:8080/job/JDA/").appendString(String.valueOf(build)).appendString("/artifact/").appendString(path).appendString(
						//									">\n");
						//						}
						//
						//						builder.appendString("\n");
						//
						//						builder.appendString("Gradle:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendCodeBlock("compile 'net.dv8tion:jda-player:" + version + "'", "gradle").appendString("\n");
						//
						//						builder.appendString("Maven:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>jda-player</artifactId>\n    <version>" + version + "</version>\n</dependency>\n",
						//								"html").appendString("\n");

						final Message message = builder.build();

						Bot.ROLE_JDA_PLAYER_UPDATES.getManager().setMentionable(true).block();

						Bot.CHANNEL_ANNOUNCEMENTS.sendMessage(message).block();

						Bot.ROLE_JDA_PLAYER_UPDATES.getManager().setMentionable(false).queue();

						Bot.config.put("jda-player.version.build", build);
						Bot.config.put("jda-player.version.name", version);

						Bot.config.save();
					}
				} catch (final Exception e) {
					Bot.LOG.fatal("The following response errored: " + response);
					Bot.LOG.log(e);
				}

				// JDA3
				try {
					response = Unirest.get("http://home.dv8tion.net:8080/job/JDA%203.x/lastBuild/api/json").asString().getBody();
					final JSONObject object = new JSONObject(response);
					final int build = Integer.valueOf(object.getString("id"));
					if (!object.getBoolean("building") && object.getString("result").equalsIgnoreCase("SUCCESS") && build != Bot.config.getInt("jda3.version.build", -1)) {
						update = true;
						final MessageBuilder builder = new MessageBuilder();

						final JSONArray artifacts = object.getJSONArray("artifacts");

						final String displayPath = artifacts.getJSONObject(0).getString("displayPath");
						String version = displayPath.substring(StringUtils.ordinalIndexOf(displayPath, ".", 2) + 1);
						version = version.substring(0, Math.min(version.indexOf('.'), version.indexOf('-')));

						final JSONArray changeSets = object.getJSONObject("changeSet").getJSONArray("items");

						builder.appendString("**JDA 3 Dev** build ").appendString(version, Formatting.BOLD).appendString(" has been released!   ").appendMention(Bot.ROLE_JDA_3_UPDATES).appendString(
								"\n\n");

						if (changeSets.length() > 0) {
							builder.appendString("Commits:", Formatting.BOLD).appendString("\n");
							for (int i = 0; i < changeSets.length(); i++) {
								final JSONObject item = changeSets.getJSONObject(i);
								final String id = item.getString("id").substring(0, 6);
								final String comment = item.getString("comment");
								final String[] lines = comment.split("\n");
								for (int j = 0; j < lines.length; j++) {
									builder.appendString(j == 0 ? id : "......", Formatting.BLOCK).appendString(" ").appendString(lines[j]).appendString("\n");
								}
							}
						}

						//						builder.appendString("\n");
						//						builder.appendString("Downloads:", Formatting.BOLD).appendString("\n");
						//
						//						builder.appendString("http://home.dv8tion.net:8080/job/JDA-Player/").appendString(String.valueOf(build)).appendString("/artifact/JDA/build/libs/ \n");
						//
						//						for (int i = 0; i < artifacts.length(); i++) {
						//							final JSONObject artifact = artifacts.getJSONObject(i);
						//							final String path = artifact.getString("relativePath");
						//							builder.appendString("<").appendString("http://home.dv8tion.net:8080/job/JDA%203.x/").appendString(String.valueOf(build)).appendString("/artifact/").appendString(path)
						//									.appendString(">\n");
						//						}
						//
						//						builder.appendString("\n");

						final Message message = builder.build();

						Bot.ROLE_JDA_3_UPDATES.getManager().setMentionable(true).block();

						Bot.CHANNEL_ANNOUNCEMENTS.sendMessage(message).block();

						Bot.ROLE_JDA_3_UPDATES.getManager().setMentionable(false).queue();

						Bot.config.put("jda3.version.build", build);
						Bot.config.put("jda3.version.name", version);

						Bot.config.save();
					}
				} catch (final Exception e) {
					Bot.LOG.fatal("The following response errored: " + response);
					Bot.LOG.log(e);
				}

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
				role = Bot.ROLE_BOTS;
			} else {
				role = Bot.ROLE_JDA_FANCLUB;
			}

			guild.getController().addRolesToMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId()
					+ ") to " + role.getName()), t -> Bot.LOG.log(t));

		}
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		final Message msg = event.getMessage();
		String text = msg.getRawContent();
		final User user = event.getAuthor();
		final Guild guild = event.getGuild();
		final MessageBuilder builder = new MessageBuilder();

		if (text.startsWith("!version")) {
			builder.appendString("Latest **JDA** version is ").appendString(Bot.config.getString("jda.version.name"), Formatting.BOLD).appendString("\n");
			builder.appendString("Latest **JDA-Player** version is ").appendString(Bot.config.getString("jda-player.version.name"), Formatting.BOLD).appendString("\n");
			builder.appendString("Latest **JDA 3** development version is ").appendString(Bot.config.getString("jda3.version.name"), Formatting.BOLD);
		} else if (text.startsWith("!shutdown") && guild.getMember(user).getRoles().contains(Bot.ROLE_STAFF)) {
			Bot.shutdown();
		} else if (text.startsWith("!docs ")) {
			text = text.substring(6);
			builder.appendString(DocParser.get(text));
		} else if (text.startsWith("!gradle")) {
			text = text.substring(7);

			if (text.contains("pretty")) {
				if (text.contains("player")) {
					builder.appendCodeBlock("compile group: 'net.dv8tion', name: 'jda-player', version: '" + Bot.config.getString("jda-player.version.name") + "'", "gradle").appendString("\n");
				} else if (text.contains("3")) {
					builder.appendCodeBlock("maven {\r\n    name \"Fab's kindly provided JDA3 Alpha maven repo\"\r\n    url \"http://nexus.notfab.net/content/repositories/JDA3\"\r\n}", "gradle");
					builder.appendString("\n");
					builder.appendCodeBlock("compile group: 'net.dv8tion', name: 'jda', version: '3.0." + Bot.config.getString("jda3.version.name") + "'", "gradle").appendString("\n");
				} else {
					builder.appendCodeBlock("compile group: 'net.dv8tion', name: 'JDA', version: '" + Bot.config.getString("jda.version.name") + "'", "gradle").appendString("\n");
				}
			} else {
				if (text.contains("player")) {
					builder.appendCodeBlock("compile 'net.dv8tion:jda-player:" + Bot.config.getString("jda-player.version.name") + "'", "gradle").appendString("\n");
				} else if (text.contains("3")) {
					builder.appendCodeBlock("maven {\r\n    name \"Fab's kindly provided JDA3 Alpha maven repo\"\r\n    url \"http://nexus.notfab.net/content/repositories/JDA3\"\r\n}", "gradle");
					builder.appendString("\n");
					builder.appendCodeBlock("compile 'net.dv8tion:jda:3.0." + Bot.config.getString("jda3.version.name") + "'", "gradle").appendString("\n");
				} else {
					builder.appendCodeBlock("compile 'net.dv8tion:JDA:" + Bot.config.getString("jda.version.name") + "'", "gradle").appendString("\n");
				}
			}

		} else if (text.startsWith("!maven")) {
			text = text.substring(6);

			if (text.contains("player")) {
				builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>jda-player</artifactId>\n    <version>" + Bot.config.getString("jda-player.version.name")
						+ "</version>\n</dependency>\n", "html").appendString("\n");
			} else if (text.contains("3")) {
				builder.appendCodeBlock("<repository>\n    <id>fabricio20</id>\n    <name>Fab's kindly provided JDA3 Alpha maven repo</name>\n    <url>http://nexus.notfab.net/content/repositories/JDA3</url>\n</repository>", "html");
				builder.appendString("\n");
				builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>jda</artifactId>\n    <version>3.0." + Bot.config.getString("jda3.version.name")
						+ "</version>\n</dependency>\n", "html").appendString("\n");
			} else {
				builder.appendCodeBlock("<dependency>\n    <groupId>net.dv8tion</groupId>\n    <artifactId>JDA</artifactId>\n    <version>" + Bot.config.getString("jda.version.name")
						+ "</version>\n</dependency>\n", "html").appendString("\n");
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
			} else if (text.contains("3")) {
				final String version = Bot.config.getString("jda3.version.name");
				final String build = Bot.config.getString("jda3.version.build");
				builder.appendString("http://home.dv8tion.net:8080/job/JDA%203.x/" + build + "/artifact/build/libs/JDA-3.0." + version + "-javadoc.jar").appendString("\n").appendString(
						"http://home.dv8tion.net:8080/job/JDA%203.x/" + build + "/artifact/build/libs/JDA-3.0." + version + "-sources.jar").appendString("\n").appendString(
								"http://home.dv8tion.net:8080/job/JDA%203.x/" + build + "/artifact/build/libs/JDA-3.0." + version + ".jar").appendString("\n").appendString(
										"http://home.dv8tion.net:8080/job/JDA%203.x/" + build + "/artifact/build/libs/JDA-withDependencies-3.0." + version + ".jar");
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
			if (text.contains("player")) {
				builder.appendCodeBlock(
						"plugins {\r\n    id 'java'\r\n    id 'application'\r\n    id 'com.github.johnrengelman.shadow' version '1.2.3'\r\n}\r\n// Change this to the name of your main class\r\nmainClassName = 'com.example.jda.Bot'\r\n\r\n// This version will be appeneded to the jar name\r\nversion '1.0'\r\n\r\ngroup 'com.mydomain.example'\r\n\r\n// The java version\r\nsourceCompatibility = 1.8\r\ntargetCompatibility = 1.8\r\n\r\nrepositories {\r\n    mavenCentral()\r\n    jcenter()\r\n}\r\n\r\n// Add your dependencies here\r\ndependencies {\r\n    compile \"net.dv8tion:JDA:"
								+ Bot.config.getString("jda.version.name") + "\"\r\n    compile \"net.dv8tion:jda-player:" + Bot.config.getString("jda-player.version.name")
								+ "\"\r\n}\r\n\r\n// Tell gradle to use UTF-8 as file encoding\r\ncompileJava.options.encoding = 'UTF-8'\r\n\r\n// This task is to set the gradle wrapper version\r\ntask wrapper(type: Wrapper) {\r\n    gradleVersion = '3.1'\r\n}",
						"gradle").appendString("\n");
			} else if (text.contains("3")) {
				builder.appendCodeBlock(
						"plugins {\r\n    id 'java'\r\n    id 'application'\r\n    id 'com.github.johnrengelman.shadow' version '1.2.3'\r\n}\r\n// Change this to the name of your main class\r\nmainClassName = 'com.example.jda.Bot'\r\n\r\n// This version will be appeneded to the jar name\r\nversion '1.0'\r\n\r\ngroup 'com.mydomain.example'\r\n\r\n// The java version\r\nsourceCompatibility = 1.8\r\ntargetCompatibility = 1.8\r\n\r\nrepositories {\r\n    mavenCentral()\r\n    jcenter()\r\n    maven {\r\n        name \"Repository for JDA 3 Alpha builds kindly provided by Fabricio20\"\r\n        url \"http://nexus.notfab.net/content/repositories/JDA3\"\r\n    }\r\n}\r\n\r\n// Add your dependencies here\r\ndependencies {\r\n    compile \"net.dv8tion:jda:3.0."
								+ Bot.config.getString("jda3.version.name")
								+ "\"\r\n}\r\n\r\n// Tell gradle to use UTF-8 as file encoding\r\ncompileJava.options.encoding = 'UTF-8'\r\n\r\n// This task is to set the gradle wrapper version\r\ntask wrapper(type: Wrapper) {\r\n    gradleVersion = '3.1'\r\n}",
						"gradle").appendString("\n");
			} else {
				builder.appendCodeBlock(
						"plugins {\r\n    id 'java'\r\n    id 'application'\r\n    id 'com.github.johnrengelman.shadow' version '1.2.3'\r\n}\r\n// Change this to the name of your main class\r\nmainClassName = 'com.example.jda.Bot'\r\n\r\n// This version will be appeneded to the jar name\r\nversion '1.0'\r\n\r\ngroup 'com.mydomain.example'\r\n\r\n// The java version\r\nsourceCompatibility = 1.8\r\ntargetCompatibility = 1.8\r\n\r\nrepositories {\r\n    mavenCentral()\r\n    jcenter()\r\n}\r\n\r\n// Add your dependencies here\r\ndependencies {\r\n    compile \"net.dv8tion:JDA:"
								+ Bot.config.getString("jda.version.name")
								+ "\"\r\n}\r\n\r\n// Tell gradle to use UTF-8 as file encoding\r\ncompileJava.options.encoding = 'UTF-8'\r\n\r\n// This task is to set the gradle wrapper version\r\ntask wrapper(type: Wrapper) {\r\n    gradleVersion = '3.1'\r\n}",
						"gradle").appendString("\n");
			}
		} else if (text.startsWith("!notify")) {
			text = text.substring(7);
			final Member member = event.getMember();
			final Role role;

			if (text.contains("player")) {
				role = Bot.ROLE_JDA_PLAYER_UPDATES;
			} else if (text.contains("3")) {
				role = Bot.ROLE_JDA_3_UPDATES;
			} else {
				role = Bot.ROLE_JDA_UPDATES;
			}

			if (member.getRoles().contains(role)) {
				guild.getController().removeRolesFromMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Removed " + user.getName() + "#" + user.getDiscriminator() + " (" + user
						.getId() + ") from " + role.getName()), t -> Bot.LOG.log(t));
			} else {
				guild.getController().addRolesToMember(member, role).queue(v -> Bot.LOG.log(SimpleLog.Level.WARNING, "Added " + user.getName() + "#" + user.getDiscriminator() + " (" + user.getId()
						+ ") to " + role.getName()), t -> Bot.LOG.log(t));
			}

		}

		if (builder.getLength() > 0) {
			event.getChannel().sendMessage(builder.build()).queue();
		}

	}
}

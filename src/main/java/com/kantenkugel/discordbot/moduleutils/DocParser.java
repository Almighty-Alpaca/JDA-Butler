/*
 * Copyright 2016 Michael Ritter (Kantenkugel) Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.kantenkugel.discordbot.moduleutils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.core.utils.SimpleLog;

public class DocParser {
	private static final SimpleLog							LOG					= SimpleLog.getLog("DocParser");

	private static final String								JDA_JENKINS_PREFIX	= "http://home.dv8tion.net:8080/job/JDA/lastSuccessfulBuild/";
	private static final String								ARTIFACT_SUFFIX		= "api/json?tree=artifacts[*]";

	private static final Path								LOCAL_SRC_PATH		= Paths.get("jda-src.jar");

	private static final String								JDA_CODE_BASE		= "net/dv8tion/jda";

	private static final Pattern							DOCS_PATTERN		= Pattern.compile("/\\*{2}\\s*\n(.*?)\n\\s*\\*/\\s*\n\\s*(?:@[^\n]+\n\\s*)*(.*?)\n", Pattern.DOTALL);
	private static final Pattern							METHOD_PATTERN		= Pattern.compile(".*?\\s([a-zA-Z][a-zA-Z0-9]*)\\(([a-zA-Z0-9\\s\\.,<>]*)\\)");
	private static final Pattern							METHOD_ARG_PATTERN	= Pattern.compile("([a-zA-Z][a-zA-Z0-9<>]*(?:\\.{3})?)\\s+[a-zA-Z][a-zA-Z0-9]");

	private static final String								LINK_PATTERN		= "\\{@link\\s.*?\\.?([^\\s\\.]+(?:\\([^\\)]*?\\))?)\\}";

	private static final Map<String, List<Documentation>>	docs				= new HashMap<>();

	private static List<String> cleanupDocs(String docs) {
		docs = docs.replace("\n", " ");
		docs = docs.replaceAll("(?:\\s+\\*)+\\s+", " ").replaceAll("\\s{2,}", " ");
		docs = docs.replaceAll("</?b>", "**").replaceAll("</?i>", "*").replaceAll("<br/?>", "\n").replaceAll("<[^>]+>", "");
		docs = docs.replaceAll("[^{]@", "\n@");
		docs = docs.replaceAll(DocParser.LINK_PATTERN, "***$1***");
		return Arrays.stream(docs.split("\n")).map(String::trim).collect(Collectors.toList());
	}

	private static void download() {
		DocParser.LOG.info("Downloading JDA sources...");
		try {
			final HttpResponse<String> response = Unirest.get(DocParser.JDA_JENKINS_PREFIX + DocParser.ARTIFACT_SUFFIX).asString();
			if (response.getStatus() < 300 && response.getStatus() > 199) {
				final JSONArray artifacts = new JSONObject(response.getBody()).getJSONArray("artifacts");
				for (int i = 0; i < artifacts.length(); i++) {
					final JSONObject artifact = artifacts.getJSONObject(i);
					if (artifact.getString("fileName").endsWith("sources.jar")) {
						final URL artifactUrl = new URL(DocParser.JDA_JENKINS_PREFIX + "artifact/" + artifact.getString("relativePath"));
						final URLConnection connection = artifactUrl.openConnection();
						connection.setConnectTimeout(5000);
						connection.setReadTimeout(5000);
						final InputStream is = connection.getInputStream();
						Files.copy(is, DocParser.LOCAL_SRC_PATH, StandardCopyOption.REPLACE_EXISTING);
						is.close();
						DocParser.LOG.info("Done downloading JDA sources");
					}
				}
			}
		} catch (UnirestException | IOException e) {
			DocParser.LOG.log(e);
		}
	}

	public static String get(final String name) {
		final String[] split = name.toLowerCase().split("[#\\.]", 2);
		if (split.length != 2)
			return "Incorrect Method declaration";
		List<Documentation> methods;
		synchronized (DocParser.docs) {
			if (!DocParser.docs.containsKey(split[0]))
				return "Class not Found!";
			methods = DocParser.docs.get(split[0]);
		}
		methods = methods.parallelStream().filter(doc -> doc.matches(split[1])).sorted(Comparator.comparingInt(doc -> doc.argTypes.size())).collect(Collectors.toList());
		if (methods.size() == 0)
			return "Method not found/documented in Class!";
		if (methods.size() > 1 && methods.get(0).argTypes.size() != 0)
			return "Multiple methods found: " + methods.parallelStream().map(m -> '(' + StringUtils.join(m.argTypes, ", ") + ')').collect(Collectors.joining(", "));
		final Documentation doc = methods.get(0);
		final StringBuilder b = new StringBuilder();
		b.append("```\n").append(doc.functionHead).append("\n```\n").append(doc.desc);
		if (doc.args.size() > 0) {
			b.append('\n').append('\n').append("**Arguments:**");
			doc.args.entrySet().stream().map(e -> "**" + e.getKey() + "** - " + e.getValue()).forEach(a -> b.append('\n').append(a));
		}
		if (doc.returns != null) {
			b.append('\n').append('\n').append("**Returns:**\n").append(doc.returns);
		}
		if (doc.throwing.size() > 0) {
			b.append('\n').append('\n').append("**Throws:**");
			doc.throwing.entrySet().stream().map(e -> "**" + e.getKey() + "** - " + e.getValue()).forEach(a -> b.append('\n').append(a));
		}
		return b.toString();
	}

	public static void init() {
		if (!DocParser.docs.isEmpty())
			return;
		DocParser.LOG.info("Initializing JDA-Docs");
		DocParser.download();
		DocParser.parse();
		DocParser.LOG.info("JDA-Docs initialized");
	}

	private static void parse() {
		DocParser.LOG.info("Parsing source-file");
		try (final JarFile file = new JarFile(DocParser.LOCAL_SRC_PATH.toFile())) {
			file.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith(DocParser.JDA_CODE_BASE) && entry.getName().endsWith(".java")).forEach(entry -> {
				try {
					DocParser.parse(entry.getName(), file.getInputStream(entry));
				} catch (final IOException e) {
					e.printStackTrace();
				}
			});
			DocParser.LOG.info("Done parsing source-file");
		} catch (final IOException e) {
			DocParser.LOG.log(e);
		}
	}

	private static void parse(final String name, final InputStream inputStream) {
		final String[] nameSplits = name.split("[/\\.]");
		final String className = nameSplits[nameSplits.length - 2];
		DocParser.docs.putIfAbsent(className.toLowerCase(), new ArrayList<>());
		final List<Documentation> docs = DocParser.docs.get(className.toLowerCase());
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
			final String content = buffer.lines().collect(Collectors.joining("\n"));
			final Matcher matcher = DocParser.DOCS_PATTERN.matcher(content);
			while (matcher.find()) {
				String method = matcher.group(2).trim();
				if (method.contains("class ") || method.contains("interface ")) {
					continue;
				}
				if (method.endsWith("{")) {
					method = method.substring(0, method.length() - 1).trim();
				}
				Matcher m2 = DocParser.METHOD_PATTERN.matcher(method);
				if (!m2.find()) {
					continue;
				}
				final String methodName = m2.group(1);
				final List<String> argTypes = new ArrayList<>();
				m2 = DocParser.METHOD_ARG_PATTERN.matcher(m2.group(2));
				while (m2.find()) {
					argTypes.add(m2.group(1));
				}
				final List<String> docText = DocParser.cleanupDocs(matcher.group(1));
				String returns = null;

				final Map<String, String> args = new HashMap<>();
				final Map<String, String> throwing = new HashMap<>();
				String desc = null;
				for (final String line : docText) {
					if (!line.isEmpty() && line.charAt(0) == '@') {
						if (line.startsWith("@return ")) {
							returns = line.substring(8);
						} else if (line.startsWith("@param ")) {
							final String[] split = line.split("\\s+", 3);
							args.put(split[1], split.length == 3 ? split[2] : "*No Description*");
						} else if (line.startsWith("@throws ")) {
							final String[] split = line.split("\\s+", 3);
							throwing.put(split[1], split.length == 3 ? split[2] : "*No Description*");
						}
					} else {
						desc = desc == null ? line : desc + '\n' + line;
					}
				}
				docs.add(new Documentation(methodName, argTypes, method, desc, returns, args, throwing));
			}
		} catch (final IOException ignored) {}
		try {
			inputStream.close();
		} catch (final IOException e) {
			DocParser.LOG.log(e);
		}
	}

	public static void reFetch() {
		try {
			DocParser.LOG.info("Re-fetching Docs");
			DocParser.download();
			synchronized (DocParser.docs) {
				DocParser.docs.clear();
				DocParser.parse();
			}
			DocParser.LOG.info("Done");
		} catch (final Exception e) {
			DocParser.LOG.log(e);
		}
	}

	private static class Documentation {
		private final String				functionName;
		private final List<String>			argTypes;
		private final String				functionHead;
		private final String				desc;
		private final String				returns;
		private final Map<String, String>	args;
		private final Map<String, String>	throwing;

		private Documentation(final String functionName, final List<String> argTypes, final String functionHead, final String desc, final String returns, final Map<String, String> args,
				final Map<String, String> throwing) {
			this.functionName = functionName;
			this.argTypes = argTypes;
			this.functionHead = functionHead;
			this.desc = desc;
			this.returns = returns;
			this.args = args;
			this.throwing = throwing;
		}

		private boolean matches(String input) {
			if (input.charAt(input.length() - 1) != ')') {
				input += "()";
			}
			final Matcher matcher = DocParser.METHOD_PATTERN.matcher(' ' + input);
			if (!matcher.find())
				return false;
			if (!matcher.group(1).equalsIgnoreCase(this.functionName))
				return false;
			final String args = matcher.group(2);
			if (args.isEmpty())
				return true;
			final String[] split = args.split(",");
			if (split.length != this.argTypes.size())
				return false;
			for (int i = 0; i < split.length; i++) {
				if (!split[i].trim().equalsIgnoreCase(this.argTypes.get(i)))
					return false;
			}
			return true;
		}
	}
}
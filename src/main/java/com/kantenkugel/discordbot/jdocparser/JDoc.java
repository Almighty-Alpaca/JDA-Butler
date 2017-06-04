/*
 *     Copyright 2016-2017 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.kantenkugel.discordbot.jdocparser;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JDoc {
    private static final Map<String, JDocParser.ClassDocumentation> docs = new HashMap<>();

    public static Message get(final String name) {
        if(name.trim().isEmpty()) {
            return new MessageBuilder().append("See the docs here: ").append(JDocUtil.JDOCBASE).build();
        }
        final String[] split = name.toLowerCase().split("[#\\.]");
        JDocParser.ClassDocumentation classDoc;
        synchronized(docs) {
            if (!docs.containsKey(split[0]))
                return new MessageBuilder().append("Class not Found!").build();
            classDoc = docs.get(split[0]);
        }
        for(int i=1; i < split.length - 1; i++) {
            if(!classDoc.subClasses.containsKey(split[i]))
                return new MessageBuilder().appendFormat("Could not find Sub-Class %s of %s", split[i], classDoc.className).build();
            classDoc = classDoc.subClasses.get(split[i]);
        }

        String searchObj = split[split.length - 1];
        if(split.length == 1 || classDoc.subClasses.containsKey(searchObj)) {
            if(split.length > 1)
                classDoc = classDoc.subClasses.get(searchObj);
            if(classDoc.isEnum) {
                Map<String, List<String>> fields = new HashMap<>();
                fields.put("Values:", classDoc.classValues.values().stream().map(valueDoc -> valueDoc.name).collect(Collectors.toList()));
                return getMessage(classDoc.classSig, classDoc.classDesc, JDocUtil.getLink(classDoc), fields);
            } else {
                return getMessage(classDoc.classSig, classDoc.classDesc, JDocUtil.getLink(classDoc));
            }
        } else if(classDoc.classValues.containsKey(searchObj)) {
            JDocParser.ValueDocumentation valueDoc = classDoc.classValues.get(searchObj);
            if(classDoc.isEnum) {
                return getMessage(classDoc.className + '.' + valueDoc.name, valueDoc.desc, JDocUtil.getLink(classDoc) + valueDoc.hashLink);
            } else {
                return getMessage(valueDoc.sig, valueDoc.desc, JDocUtil.getLink(classDoc) + valueDoc.hashLink);
            }
        } else {
            boolean fuzzy = false;
            if(searchObj.charAt(searchObj.length() - 1) != ')') {
                searchObj += "()";
                fuzzy = true;
            }
            final String methodSig = searchObj;
            final boolean fuzzySearch = fuzzy;
            String[] methodParts = methodSig.split("[\\(\\)]");
            String methodName = methodParts[0];
            if(classDoc.methodDocs.containsKey(methodName.toLowerCase())) {
                List<JDocParser.MethodDocumentation> docs = classDoc.methodDocs.get(methodName.toLowerCase()).parallelStream()
                        .filter(doc -> doc.matches(methodSig, fuzzySearch))
                        .sorted(Comparator.comparingInt(doc -> doc.argTypes.size()))
                        .collect(Collectors.toList());
                if(docs.size() == 1) {
                    JDocParser.MethodDocumentation doc = docs.get(0);
                    return getMessage(doc.functionSig, doc.desc, JDocUtil.getLink(classDoc) + doc.hashLink, doc.fields);
                } else if(docs.size() == 0) {
                    return new MessageBuilder().append("Found methods with given name but no matching signature").build();
                } else {
                    String methods = docs.stream()
                            .map(doc -> doc.functionSig)
                            .collect(Collectors.joining("\n"));
                    return new MessageBuilder().append("Found multiple valid method signatures: ```").append(methods).append("```").build();
                }
            }
            return new MessageBuilder().append("Could not find search-query").build();
        }
    }

    private static Message getMessage(String title, String description, String linkUrl) {
        return getMessage(title, description, linkUrl, null);
    }

    private static Message getMessage(String title, String description, String linkUrl, Map<String, List<String>> fields) {
        EmbedBuilder embedBuilder = new EmbedBuilder().setTitle(title, linkUrl);
        if(description.length() > MessageEmbed.TEXT_MAX_LENGTH) {
            embedBuilder.setDescription("Description to long. please refer to [the docs](" + linkUrl + ')');
        } else {
            embedBuilder.setDescription(description);
        }
        if(fields != null && fields.size() > 0) {
            for(Map.Entry<String, List<String>> field : fields.entrySet()) {
                embedBuilder.addField(field.getKey(), field.getValue().stream().collect(Collectors.joining("\n")), false);
            }
        }
        return new MessageBuilder().setEmbed(embedBuilder.build()).build();
    }

    public static void init() {
        if (!docs.isEmpty())
            return;
        JDocUtil.LOG.info("Initializing JDA-Docs");
        download();
        fetch();
        JDocUtil.LOG.info("JDA-Docs initialized");
    }

    public static void reFetch() {
        try {
            JDocUtil.LOG.info("Re-fetching Docs");
            download();
            synchronized (docs) {
                docs.clear();
            }
            fetch();
            JDocUtil.LOG.info("Done");
        } catch (final Exception e) {
            JDocUtil.LOG.log(e);
        }
    }

    private static void fetch() {
        Map<String, JDocParser.ClassDocumentation> newDocs = JDocParser.parse();
        synchronized(docs) {
            docs.putAll(newDocs);
        }
    }

    private static void download() {
        JDocUtil.LOG.info("Downloading JDA docs...");
        try {
            final HttpResponse<String> response = Unirest.get(JDocUtil.ARTIFACT_URL).asString();
            if (response.getStatus() < 300 && response.getStatus() > 199) {
                final JSONArray artifacts = new JSONObject(response.getBody()).getJSONArray("artifacts");
                for (int i = 0; i < artifacts.length(); i++) {
                    final JSONObject artifact = artifacts.getJSONObject(i);
                    if (artifact.getString("fileName").endsWith("javadoc.jar")) {
                        if(download(artifact.getString("relativePath"), JDocUtil.LOCAL_DOC_PATH))
                            JDocUtil.LOG.info("Done downloading JDA docs");
                        break;
                    }
                }
            }
        } catch (UnirestException e) {
            JDocUtil.LOG.log(e);
        }
    }

    private static boolean download(String relPath, Path destination) {
        try {
            final URL artifactUrl = new URL(JDocUtil.JENKINSBASE + "artifact/" + relPath);
            final URLConnection connection = artifactUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            final InputStream is = connection.getInputStream();
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return true;
        } catch(IOException e) {
            JDocUtil.LOG.log(e);
        }
        return false;
    }
}

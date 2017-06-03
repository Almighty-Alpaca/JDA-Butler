/*
 *     Copyright 2016 Michael Ritter (Kantenkugel)
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

package com.kantenkugel.discordbot.moduleutils;

import com.almightyalpaca.discord.jdabutler.JDAUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
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

public class DocParser {
    private static final SimpleLog LOG = SimpleLog.getLog("DocParser");

    private static final String ARTIFACT_SUFFIX = "api/json?tree=artifacts[*]";

    private static final Path LOCAL_DOC_PATH = Paths.get("jda-docs.jar");

    private static final String JDA_CODE_BASE = "net/dv8tion/jda";

    public static final Pattern METHOD_PATTERN = Pattern.compile("([a-zA-Z][a-zA-Z0-9]+)\\(([a-zA-Z0-9\\s\\.,<>\\[\\]]*)\\)");
    public static final Pattern METHOD_ARG_PATTERN = Pattern.compile("\\s*([a-zA-Z][a-zA-Z0-9\\.]*)\\s+([a-zA-Z][a-zA-Z0-9]*)(?:\\s*,|$)");

    private static final Pattern LINK_PATTERN = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>");
    private static final Pattern CODE_PATTERN = Pattern.compile("<code>(.*?)</code>");

    public static final Map<String, ClassDocumentation> docs = new HashMap<>();

    public static Message get(final String name) {
        final String[] split = name.toLowerCase().split("[#\\.]");
        System.out.println(Arrays.toString(split));
        ClassDocumentation classDoc;
        synchronized (DocParser.docs) {
            if (!DocParser.docs.containsKey(split[0]))
                return new MessageBuilder().append("Class not Found!").build();
            classDoc = DocParser.docs.get(split[0]);
        }
        for(int i=1; i < split.length - 1; i++) {
            if(!classDoc.subClasses.containsKey(split[i]))
                return new MessageBuilder().appendFormat("Could not find Sub-Class %s of %s", split[i], classDoc.className).build();
            classDoc = classDoc.subClasses.get(split[i]);
        }

        System.out.println("Got Class " + classDoc.classSig);
        if(split.length == 1) {
            return getMessage(
                    new EmbedBuilder()
                            .setTitle(classDoc.classSig, getLink(classDoc)),
                    classDoc.classDesc,
                    classDoc
            );
        }

        String searchObj = split[split.length - 1];
        System.out.println("Search-object is: " + searchObj);
        if(classDoc.subClasses.containsKey(searchObj)) {
            classDoc = classDoc.subClasses.get(searchObj);
            return getMessage(
                    new EmbedBuilder()
                            .setTitle(classDoc.classSig, getLink(classDoc)),
                    classDoc.classDesc,
                    classDoc
            );
        } else if(classDoc.classValues.containsKey(searchObj)) {
            ValueDocumentation valueDoc = classDoc.classValues.get(searchObj);
            if(classDoc.isEnum) {
                return getMessage(
                        new EmbedBuilder()
                                .setTitle(classDoc.className+'.'+valueDoc.name, getLink(classDoc)),
                        valueDoc.desc,
                        classDoc
                );
            } else {
                return getMessage(
                        new EmbedBuilder()
                                .setTitle(valueDoc.sig, getLink(classDoc)),
                        valueDoc.desc,
                        classDoc
                );
            }
        } else {
            boolean fuzzy = false;
            if(searchObj.charAt(searchObj.length() - 1) != ')') {
                searchObj += "()";
                fuzzy = true;
            }
            final String methodSig = searchObj;
            final boolean fuzzySearch = fuzzy;
            Matcher matcher = METHOD_PATTERN.matcher(searchObj);
            if(matcher.find()) {
                String methodName = matcher.group(1);
                if(classDoc.methodDocs.containsKey(methodName.toLowerCase())) {
                    List<MethodDocumentation> docs = classDoc.methodDocs.get(methodName.toLowerCase()).parallelStream()
                            .filter(doc -> doc.matches(methodSig, fuzzySearch))
                            .sorted(Comparator.comparingInt(doc -> doc.argTypes.size()))
                            .collect(Collectors.toList());
                    if(docs.size() == 1) {
                        MethodDocumentation doc = docs.get(0);
                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setTitle(doc.functionSig, getLink(classDoc));
                        for(Map.Entry<String, List<String>> fieldEntry : doc.fields.entrySet()) {
                            embedBuilder.addField(fieldEntry.getKey(), fieldEntry.getValue().stream().collect(Collectors.joining("\n")), false);
                        }
                        return getMessage(embedBuilder, doc.desc, classDoc);
                    } else if(docs.size() == 0) {
                        return new MessageBuilder().append("Found methods with given name but no matching signature").build();
                    } else {
                        String methods = docs.stream()
                                .map(doc -> doc.functionName + '(' + doc.argTypes.stream().collect(Collectors.joining(", ")) + ')')
                                .collect(Collectors.joining(", "));
                        return new MessageBuilder().append("Found multiple valid method signatures: ").append(methods).build();
                    }
                }
            }
            return new MessageBuilder().append("Could not find search-query").build();
        }
    }

    private static Message getMessage(EmbedBuilder builder, String description, ClassDocumentation reference) {
        try {
            builder.setDescription(description);
        } catch(IllegalArgumentException ex) {
            builder.setDescription("Description to long. please refer to [the docs](" + getLink(reference) + ')');
        }
        return new MessageBuilder().setEmbed(builder.build()).build();
    }

    public static void init() {
        if (!DocParser.docs.isEmpty())
            return;
        DocParser.LOG.info("Initializing JDA-Docs");
        DocParser.download();
        DocParser.parse();
        DocParser.LOG.info("JDA-Docs initialized");
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

    private static String cleanupText(String docs, String currentUrl) {
        docs = replaceUglySpaces(docs);
        docs = docs.replace("\n", " ").replaceAll("\\s{2,}", " ");
        docs = docs.replaceAll("</?b>", "**").replaceAll("</?i>", "*").replaceAll("<br\\s?/?>", "\n");
        docs = CODE_PATTERN.matcher(docs).replaceAll("***$1***");
        Matcher matcher = LINK_PATTERN.matcher(docs);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(sb, '[' + matcher.group(2) + "](" + resolveLink(matcher.group(1), currentUrl) + ')');
        }
        matcher.appendTail(sb);
        docs = sb.toString();
        docs = docs.replaceAll("<[^>]+>", "");
        return Arrays.stream(docs.split("\n")).map(String::trim).collect(Collectors.joining("\n"));
    }

    private static String getLink(ClassDocumentation doc) {
        return getLink(doc.pack, doc.className);
    }

    private static String getLink(String classPackage, String className) {
        return getPathToLastJenkinsBuild() + "javadoc/" + classPackage.replace(".", "/") + '/' + className + ".html";
    }

    private static String resolveLink(String href, String relativeTo) {
        try {
            URL base = new URL(relativeTo);
            URL result = new URL(base, href);
            return result.toString();
        } catch(MalformedURLException e) {
            LOG.log(e);
        }
        return null;
    }

    private static String getPathToLastJenkinsBuild() {
        return "http://" + JDAUtil.JENKINS_BASE.get() + ":8080/job/JDA/lastSuccessfulBuild/";
    }

    private static void download() {
        DocParser.LOG.info("Downloading JDA docs...");
        try {
            final HttpResponse<String> response = Unirest.get(DocParser.getPathToLastJenkinsBuild() + DocParser.ARTIFACT_SUFFIX).asString();
            if (response.getStatus() < 300 && response.getStatus() > 199) {
                final JSONArray artifacts = new JSONObject(response.getBody()).getJSONArray("artifacts");
                for (int i = 0; i < artifacts.length(); i++) {
                    final JSONObject artifact = artifacts.getJSONObject(i);
                    if (artifact.getString("fileName").endsWith("javadoc.jar")) {
                        if(download(artifact.getString("relativePath"), DocParser.LOCAL_DOC_PATH))
                            DocParser.LOG.info("Done downloading JDA docs");
                        break;
                    }
                }
            }
        } catch (UnirestException e) {
            DocParser.LOG.log(e);
        }
    }

    private static boolean download(String relPath, Path destination) {
        try {
            final URL artifactUrl = new URL(DocParser.getPathToLastJenkinsBuild() + "artifact/" + relPath);
            final URLConnection connection = artifactUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            final InputStream is = connection.getInputStream();
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return true;
        } catch(IOException e) {
            DocParser.LOG.log(e);
        }
        return false;
    }

    private static void parse() {
        DocParser.LOG.info("Parsing docs-files");
        try (final JarFile file = new JarFile(DocParser.LOCAL_DOC_PATH.toFile())) {
            file.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith(DocParser.JDA_CODE_BASE) && entry.getName().endsWith(".html")).forEach(entry -> {
                try {
                    DocParser.parse(entry.getName(), file.getInputStream(entry));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });
            DocParser.LOG.info("Done parsing docs-files");
        } catch (final IOException e) {
            DocParser.LOG.log(e);
        }
    }

    private static Element getSingleElementByClass(Element root, String className) {
        Elements elementsByClass = root.getElementsByClass(className);
        if(elementsByClass.size() != 1) {
            String error = "Found " + elementsByClass.size() + " elements with class " + className + " inside of " + root.tagName() + "-" + root.className();
            DocParser.LOG.fatal(error);
            throw new RuntimeException(error + root.html());
        }
        return elementsByClass.size() == 0 ? null : elementsByClass.get(0);
    }
    private static Element getSingleElementByQuery(Element root, String query) {
        Elements elementsByClass = root.select(query);
        if(elementsByClass.size() > 1) {
            String error = "Found " + elementsByClass.size() + " elements matching query \"" + query + "\" inside of " + root.tagName() + "-" + root.className();
            DocParser.LOG.fatal(error);
            throw new RuntimeException(error + root.html());
        }
        return elementsByClass.size() == 0 ? null : elementsByClass.get(0);
    }

    private static String replaceUglySpaces(String input) {
        return input == null ? null : input.replaceAll("[\\h\\s]", " ");
    }

    private static void parse(final String name, final InputStream inputStream) {
        final String[] pathSplits = name.split("/");
        final String fileName = pathSplits[pathSplits.length - 1];
        if(!Character.isUpperCase(fileName.charAt(0))) {
            //ignore jdoc structure html
            return;
        }
        final String[] nameSplits = fileName.split("\\.");
        final String className = nameSplits[nameSplits.length - 2];
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
            final String content = buffer.lines().collect(Collectors.joining("\n"));
            Document document = Jsoup.parse(content);
            Element titleElem = getSingleElementByClass(document, "title");
            final String classSig = replaceUglySpaces(titleElem.text());
            final String pack = replaceUglySpaces(titleElem.previousElementSibling().text());
            final String link = getLink(pack, className);
            final String description = cleanupText(getSingleElementByQuery(document, ".description .block").html(), link);
            final ClassDocumentation classDoc = new ClassDocumentation(pack, className, classSig, description, classSig.startsWith("Enum"));
            final Element details = document.getElementsByClass("details").first();
            //methods
            Element tmp = getSingleElementByQuery(details, "a[name=\"method.detail\"]");
            List<DocBlock> docBlock = getDocBlock(tmp, classDoc);
            if(docBlock != null) {
                for(DocBlock block : docBlock) {
                    if(!classDoc.methodDocs.containsKey(block.title.toLowerCase()))
                        classDoc.methodDocs.put(block.title.toLowerCase(), new HashSet<>());
                    classDoc.methodDocs.get(block.title.toLowerCase()).add(new MethodDocumentation(block.title, block.signature, block.description, block.fields));
                }
            }
            //vars
            tmp = getSingleElementByQuery(details, "a[name=\"field.detail\"]");
            docBlock = getDocBlock(tmp, classDoc);
            if(docBlock != null) {
                for(DocBlock block : docBlock) {
                    classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(block.title, block.signature, block.description));
                }
            }
            //enum-values
            tmp = getSingleElementByQuery(details, "a[name=\"enum.constant.detail\"]");
            docBlock = getDocBlock(tmp, classDoc);
            if(docBlock != null) {
                for(DocBlock block : docBlock) {
                    classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(block.title, block.signature, block.description));
                }
            }

            //storing
            if(nameSplits.length > 2) {
                if(!docs.containsKey(nameSplits[0].toLowerCase()))
                    docs.put(nameSplits[0].toLowerCase(), new ClassDocumentation(null, null, null, null, false));
                ClassDocumentation parent = docs.get(nameSplits[0].toLowerCase());
                for(int i = 1; i < nameSplits.length - 2; i++) {
                    if(!parent.subClasses.containsKey(nameSplits[i].toLowerCase()))
                        parent.subClasses.put(nameSplits[i].toLowerCase(), new ClassDocumentation(null, null, null, null, false));
                    parent = parent.subClasses.get(nameSplits[i].toLowerCase());
                }
                if(parent.subClasses.containsKey(className.toLowerCase()))
                    classDoc.subClasses.putAll(parent.subClasses.get(className.toLowerCase()).subClasses);
                parent.subClasses.put(className.toLowerCase(), classDoc);
            } else {
                if(docs.containsKey(className.toLowerCase()))
                    classDoc.subClasses.putAll(docs.get(className.toLowerCase()).subClasses);
                docs.put(className.toLowerCase(), classDoc);
            }
        } catch (final IOException | NullPointerException ignored) {}
        try {
            inputStream.close();
        } catch (final IOException e) {
            DocParser.LOG.log(e);
        }
    }

    private static List<DocBlock> getDocBlock(Element root, ClassDocumentation reference) {
        if(root != null) {
            List<DocBlock> blocks = new ArrayList<>(10);
            root.siblingElements().stream().filter(sibling -> sibling.tagName().equals("ul")).forEach(sibling -> {
                Element tmp2 = sibling.getElementsByTag("h4").first();
                String title = replaceUglySpaces(tmp2.text().trim());
                String description = null, signature = null;
                OrderedMap<String, List<String>> fields = new ListOrderedMap<>();
                for(;tmp2 != null; tmp2 = tmp2.nextElementSibling()) {
                    if(tmp2.tagName().equals("pre")) {
                        signature = replaceUglySpaces(tmp2.text().trim());
                    } else if(tmp2.tagName().equals("div") && tmp2.className().equals("block")) {
                        description = cleanupText(tmp2.html(), getLink(reference));
                    } else if(tmp2.tagName().equals("dl")) {
                        String fieldName = null;
                        List<String> fieldValues = new ArrayList<>();
                        for(Element element : tmp2.children()) {
                            if(element.tagName().equals("dt")) {
                                if(fieldName != null) {
                                    fields.put(fieldName, fieldValues);
                                    fieldValues = new ArrayList<>();
                                }
                                fieldName = replaceUglySpaces(element.text().trim());
                            } else if(element.tagName().equals("dd")) {
                                fieldValues.add(cleanupText(element.html(), getLink(reference)));
                            }
                        }
                        if(fieldName != null) {
                            fields.put(fieldName, fieldValues);
                        }
                    }
                }
                blocks.add(new DocBlock(title, signature, description, fields));
            });
            return blocks;
        }
        return null;
    }

    private static class DocBlock {
        private final String                            title;
        private final String                            signature;
        private final String                            description;
        private final OrderedMap<String, List<String>>  fields;

        private DocBlock(String title, String signature, String description, OrderedMap<String, List<String>> fields) {
            this.title = title;
            this.signature = signature;
            this.description = description;
            this.fields = fields;
        }
    }

    private static class ClassDocumentation {
        private final String                                pack;
        private final String                                className;
        private final String                                classSig;
        private final String                                classDesc;
        private final boolean                               isEnum;
        private final Map<String, Set<MethodDocumentation>> methodDocs = new HashMap<>();
        private final Map<String, ClassDocumentation>       subClasses = new HashMap<>();
        private final Map<String, ValueDocumentation>       classValues = new HashMap<>();

        private ClassDocumentation(String pack, String className, String classSig, String classDesc, boolean isEnum) {
            this.pack = pack;
            this.className = className;
            this.classSig = classSig;
            this.classDesc = classDesc;
            this.isEnum = isEnum;
        }
    }

    private static class MethodDocumentation {
        private final String                            functionName;
        private final String                            functionSig;
        private final List<String>                      argTypes;
        private final String                            desc;
        private final OrderedMap<String, List<String>>  fields;

        private MethodDocumentation(final String functionName, final String functionSig, final String desc, final OrderedMap<String, List<String>> fields) {
            this.functionName = functionName;
            this.functionSig = functionSig;
            this.desc = desc;
            this.fields = fields;
            Matcher matcher = METHOD_PATTERN.matcher(functionSig);
            if(!matcher.find()) {
                System.out.println('"' + functionSig + '"');
                throw new RuntimeException("Got method with no proper method signature: " + functionSig);
            }
            Matcher matcher2 = METHOD_ARG_PATTERN.matcher(matcher.group(2));
            this.argTypes = new ArrayList<>(3);

            while(matcher2.find()) {
                this.argTypes.add(matcher2.group(1));
            }
        }

        private boolean matches(String input, boolean fuzzy) {
            final Matcher matcher = DocParser.METHOD_PATTERN.matcher(input);
            if (!matcher.find())
                return false;
            if (!matcher.group(1).equalsIgnoreCase(this.functionName))
                return false;
            final String args = matcher.group(2);
            if (fuzzy)
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

    private static class ValueDocumentation {
        private final String name;
        private final String sig;
        private final String desc;

        private ValueDocumentation(String name, String sig, String desc) {
            this.name = name;
            this.sig = sig;
            this.desc = desc;
        }
    }
}
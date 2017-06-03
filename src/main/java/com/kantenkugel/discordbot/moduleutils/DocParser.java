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

    //return, funcName, parameters
    public static final Pattern METHOD_PATTERN = Pattern.compile("([a-zA-Z\\.<>\\[\\]]+)\\s+([a-zA-Z][a-zA-Z0-9]+)\\(([a-zA-Z0-9\\s\\.,<>\\[\\]]*)\\)");
    //type, name
    public static final Pattern METHOD_ARG_PATTERN = Pattern.compile("\\s*(?:[a-z]+\\.)*([a-zA-Z][a-zA-Z0-9\\.<>\\[\\]]*)\\s+([a-zA-Z][a-zA-Z0-9]*)(?:\\s*,|$)");

    private static final Pattern LINK_PATTERN = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>");
    private static final Pattern CODE_PATTERN = Pattern.compile("<code>(.*?)</code>");

    public static final Map<String, ClassDocumentation> docs = new HashMap<>();

    public static Message get(final String name) {
        if(name.trim().isEmpty()) {
            return new MessageBuilder().append("See the docs here: ").append(getPathToLastJenkinsBuild()).append("javadoc/").build();
        }
        final String[] split = name.toLowerCase().split("[#\\.]");
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

        String searchObj = split[split.length - 1];
        if(split.length == 1 || classDoc.subClasses.containsKey(searchObj)) {
            if(split.length > 1)
                classDoc = classDoc.subClasses.get(searchObj);
            if(classDoc.isEnum) {
                Map<String, List<String>> fields = new HashMap<>();
                fields.put("Values:", classDoc.classValues.values().stream().map(valueDoc -> valueDoc.name).collect(Collectors.toList()));
                return getMessage(classDoc.classSig, classDoc.classDesc, getLink(classDoc), fields);
            } else {
                return getMessage(classDoc.classSig, classDoc.classDesc, getLink(classDoc));
            }
        } else if(classDoc.classValues.containsKey(searchObj)) {
            ValueDocumentation valueDoc = classDoc.classValues.get(searchObj);
            if(classDoc.isEnum) {
                return getMessage(classDoc.className + '.' + valueDoc.name, valueDoc.desc, getLink(classDoc) + valueDoc.hashLink);
            } else {
                return getMessage(valueDoc.sig, valueDoc.desc, getLink(classDoc) + valueDoc.hashLink);
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
                List<MethodDocumentation> docs = classDoc.methodDocs.get(methodName.toLowerCase()).parallelStream()
                        .filter(doc -> doc.matches(methodSig, fuzzySearch))
                        .sorted(Comparator.comparingInt(doc -> doc.argTypes.size()))
                        .collect(Collectors.toList());
                if(docs.size() == 1) {
                    MethodDocumentation doc = docs.get(0);
                    return getMessage(doc.functionSig, doc.desc, getLink(classDoc) + doc.hashLink, doc.fields);
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
                    LOG.log(e);
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
            Element descriptionElement = getSingleElementByQuery(document, ".description .block");
            final String description = descriptionElement == null ? "" : cleanupText(descriptionElement.html(), link);
            final ClassDocumentation classDoc = new ClassDocumentation(pack, className, classSig, description, classSig.startsWith("Enum"));
            final Element details = document.getElementsByClass("details").first();
            if(details != null) {
                //methods
                Element tmp = getSingleElementByQuery(details, "a[name=\"method.detail\"]");
                List<DocBlock> docBlock = getDocBlock(tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        if(!classDoc.methodDocs.containsKey(block.title.toLowerCase()))
                            classDoc.methodDocs.put(block.title.toLowerCase(), new HashSet<>());
                        classDoc.methodDocs.get(block.title.toLowerCase()).add(new MethodDocumentation(block.signature, block.hashLink, block.description, block.fields));
                    }
                }
                //vars
                tmp = getSingleElementByQuery(details, "a[name=\"field.detail\"]");
                docBlock = getDocBlock(tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(block.title, block.hashLink, block.signature, block.description));
                    }
                }
                //enum-values
                tmp = getSingleElementByQuery(details, "a[name=\"enum.constant.detail\"]");
                docBlock = getDocBlock(tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(block.title, block.hashLink, block.signature, block.description));
                    }
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
        } catch (final IOException | NullPointerException ex) {
            LOG.fatal("Got excaption for element " + className);
            LOG.log(ex);
        }
        try {
            inputStream.close();
        } catch (final IOException e) {
            DocParser.LOG.log(e);
        }
    }

    private static List<DocBlock> getDocBlock(Element elem, ClassDocumentation reference) {
        if(elem != null) {
            List<DocBlock> blocks = new ArrayList<>(10);
            String hashLink = null;
            for(elem = elem.nextElementSibling(); elem != null; elem = elem.nextElementSibling()) {
                if(elem.tagName().equals("a")) {
                    hashLink = '#' + elem.attr("name");
                } else if(elem.tagName().equals("ul")) {
                    Element tmp = elem.getElementsByTag("h4").first();
                    String title = replaceUglySpaces(tmp.text().trim());
                    String description = null, signature = null;
                    OrderedMap<String, List<String>> fields = new ListOrderedMap<>();
                    for(;tmp != null; tmp = tmp.nextElementSibling()) {
                        if(tmp.tagName().equals("pre")) {
                            signature = replaceUglySpaces(tmp.text().trim());
                        } else if(tmp.tagName().equals("div") && tmp.className().equals("block")) {
                            description = cleanupText(tmp.html(), getLink(reference));
                        } else if(tmp.tagName().equals("dl")) {
                            String fieldName = null;
                            List<String> fieldValues = new ArrayList<>();
                            for(Element element : tmp.children()) {
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
                    blocks.add(new DocBlock(title, hashLink, signature, description, fields));
                }
            }
            return blocks;
        }
        return null;
    }

    private static class DocBlock {
        private final String                            title;
        private final String                            hashLink;
        private final String                            signature;
        private final String                            description;
        private final OrderedMap<String, List<String>>  fields;

        private DocBlock(String title, String hashLink, String signature, String description, OrderedMap<String, List<String>> fields) {
            this.title = title;
            this.hashLink = hashLink;
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
        private final String                            hashLink;
        private final String                            functionSig;
        private final List<String>                      argTypes;
        private final String                            desc;
        private final OrderedMap<String, List<String>>  fields;

        private MethodDocumentation(String functionSig, final String hashLink, final String desc, final OrderedMap<String, List<String>> fields) {
            functionSig = functionSig.replaceAll("(?:[a-z]+\\.)+([A-Z])", "$1").replaceAll("\\s{2,}", " ");
            Matcher methodMatcher = METHOD_PATTERN.matcher(functionSig);
            if(!methodMatcher.find()) {
                System.out.println('"' + functionSig + '"');
                throw new RuntimeException("Got method with no proper method signature: " + functionSig);
            }
            this.functionName = methodMatcher.group(2);
            this.hashLink = hashLink;
            this.functionSig = methodMatcher.group();
            this.desc = desc;
            this.fields = fields;

            String args = methodMatcher.group(3);
            Matcher argMatcher = METHOD_ARG_PATTERN.matcher(args);
            this.argTypes = new ArrayList<>(3);

            while(argMatcher.find()) {
                this.argTypes.add(argMatcher.group(1).toLowerCase().split("<")[0]);
            }

            if(!args.isEmpty() && this.argTypes.size() == 0) {
                throw new RuntimeException("Got non-empty parameters for method " + functionName + " but couldn't parse them. Signature: \"" + functionSig + '\"');
            }
        }

        private boolean matches(String input, boolean fuzzy) {
            final Matcher matcher = DocParser.METHOD_PATTERN.matcher("ff " + input);
            if (!matcher.find())
                return false;
            if (!matcher.group(2).equalsIgnoreCase(this.functionName))
                return false;
            if (fuzzy)
                return true;
            final String args = matcher.group(3);
            final String[] split = args.toLowerCase().split(",");
            int argLength = args.trim().isEmpty() ? 0 : split.length;
            if (argLength != this.argTypes.size())
                return false;
            for (int i = 0; i < this.argTypes.size(); i++) {
                if (!split[i].trim().equals(this.argTypes.get(i)))
                    return false;
            }
            return true;
        }
    }

    private static class ValueDocumentation {
        private final String name;
        private final String hashLink;
        private final String sig;
        private final String desc;

        private ValueDocumentation(String name, String hashLink, String sig, String desc) {
            this.name = name;
            this.hashLink = hashLink;
            this.sig = sig;
            this.desc = desc;
        }
    }
}
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

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JDocParser {

    //return, funcName, parameters
    public static final Pattern METHOD_PATTERN = Pattern.compile("([a-zA-Z.<>?\\[\\]]+)\\s+([a-zA-Z][a-zA-Z0-9]+)\\(([@a-zA-Z0-9\\s.,<>?\\[\\]]*)\\)");

    //annotations in front of method
    public static final Pattern ANNOTATION_PATTERN = Pattern.compile("^((?:@[^\n]+\n)+)");
    //annotation splitter
    public static final Pattern ANNOTATION_PARTS = Pattern.compile("@([a-zA-Z]+)(\\(\\S*\\))?\n");
    //type, name
    public static final Pattern METHOD_ARG_PATTERN = Pattern.compile("(?:[a-z]+\\.)*([a-zA-Z][a-zA-Z0-9.<,?>\\[\\]]*)\\s+([a-zA-Z][a-zA-Z0-9]*)(?:\\s*,|$)");

    //used for inner classes being made available as top-level search if applicable
    static final String SUBCLASSES_MAP_KEY = "#JDOC_SUBCLASSES_KEY#";

    static Map<String, ClassDocumentation> parse() {
        JDocUtil.LOG.debug("Parsing jda-docs-files");
        Map<String, ClassDocumentation> docs = new HashMap<>();
        try (final JarFile file = new JarFile(JDocUtil.LOCAL_DOC_PATH.toFile())) {
            file.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith(JDocUtil.JDA_CODE_BASE) && entry.getName().endsWith(".html")).forEach(entry -> {
                try {
                    parse(JDocUtil.JDOCBASE, entry.getName(), file.getInputStream(entry), docs);
                } catch (final IOException e) {
                    JDocUtil.LOG.error("Error while parsing doc file {}", entry.getName(), e);
                }
            });
            ClassDocumentation subClassesNode = docs.remove(SUBCLASSES_MAP_KEY);
            if(subClassesNode != null) {
                subClassesNode.subClasses.forEach((subclassName, subClassDoc) -> {
                    if(subClassDoc.classSig != null && !docs.containsKey(subclassName))
                        docs.put(subclassName, subClassDoc);
                });
            }
            JDocUtil.LOG.debug("Done parsing jda-docs-files");
        } catch (final Exception e) {
            JDocUtil.LOG.error("Error reading the jdoc jarfile", e);
        }
        return docs;
    }

    private static Element getSingleElementByClass(Element root, String className) {
        Elements elementsByClass = root.getElementsByClass(className);
        if(elementsByClass.size() != 1) {
            String error = "Found " + elementsByClass.size() + " elements with class " + className + " inside of " + root.tagName() + "-" + root.className();
            throw new RuntimeException(error + root.html());
        }
        return elementsByClass.first();
    }

    private static Element getSingleElementByQuery(Element root, String query) {
        Elements elementsByQuery = root.select(query);
        if(elementsByQuery.size() > 1) {
            String error = "Found " + elementsByQuery.size() + " elements matching query \"" + query + "\" inside of " + root.tagName() + "-" + root.className();
            throw new RuntimeException(error + root.html());
        }
        return elementsByQuery.first();
    }

    static void parse(final String jdocBase, final String name, final InputStream inputStream, Map<String, ClassDocumentation> docs) {
        final String[] pathSplits = name.split("/");
        final String fileName = pathSplits[pathSplits.length - 1];
        if(!Character.isUpperCase(fileName.charAt(0))) {
            //ignore jdoc structure html
            return;
        }
        final String[] nameSplits = fileName.split("\\.");
        final String className = nameSplits[nameSplits.length - 2];
        final String fullName = fileName.substring(0, fileName.length() - nameSplits[nameSplits.length - 1].length() - 1);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream))) {
            //create dom Document
            final String content = buffer.lines().collect(Collectors.joining("\n"));
            Document document = Jsoup.parse(content);

            //classDocument (classname, package, description)
            Element titleElem = getSingleElementByClass(document, "title");
            final String classSig = JDocUtil.fixSpaces(titleElem.text());
            Element packageElem = titleElem.previousElementSibling();
            if(packageElem.children().size() > 1) {
                packageElem = packageElem.children().last();
            }
            final String pack = JDocUtil.fixSpaces(packageElem.text());
            final String link = JDocUtil.getLink(jdocBase, pack, fullName);
            Element descriptionElement = null;
            Elements descriptionCandidates = document.select(".description .block");
            if(descriptionCandidates.size() > 1) {
                List<Element> removed = descriptionCandidates
                        .stream().map(elem -> elem.child(0))
                        .filter(child -> child != null && !child.className().startsWith("deprecat"))
                        .map(Element::parent).collect(Collectors.toList());
                if(removed.size() != 1)
                    throw new RuntimeException("Found too many description candidates");
                descriptionElement = removed.get(0);
            } else if(descriptionCandidates.size() == 1) {
                descriptionElement = descriptionCandidates.get(0);
            }
            final String description = descriptionElement == null ? "" : JDocUtil.formatText(descriptionElement.html(), link);
            final ClassDocumentation classDoc = new ClassDocumentation(pack, fullName, classSig, description, classSig.startsWith("Enum"));

            //methods, fields
            final Element details = document.getElementsByClass("details").first();
            if(details != null) {
                //methods
                Element tmp = getSingleElementByQuery(details, "a[name=\"method.detail\"]");
                List<DocBlock> docBlock = getDocBlock(jdocBase, tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        Set<MethodDocumentation> mdocs = classDoc.methodDocs.computeIfAbsent(block.title.toLowerCase(), key -> new HashSet<>());
                        mdocs.add(new MethodDocumentation(classDoc, block.signature, block.hashLink, block.description, block.fields));
                    }
                }
                //vars
                tmp = getSingleElementByQuery(details, "a[name=\"field.detail\"]");
                docBlock = getDocBlock(jdocBase, tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(classDoc, block.title, block.hashLink, block.signature, block.description));
                    }
                }
                //enum-values
                tmp = getSingleElementByQuery(details, "a[name=\"enum.constant.detail\"]");
                docBlock = getDocBlock(jdocBase, tmp, classDoc);
                if(docBlock != null) {
                    for(DocBlock block : docBlock) {
                        classDoc.classValues.put(block.title.toLowerCase(), new ValueDocumentation(classDoc, block.title, block.hashLink, block.signature, block.description));
                    }
                }
            }
            final Element methodSummary = getSingleElementByQuery(document, "a[name=\"method.summary\"]");
            classDoc.inheritedMethods.putAll(getInheritedMethods(methodSummary));

            //storing
            if(nameSplits.length > 2) {
                ClassDocumentation parent = docs.computeIfAbsent(nameSplits[0].toLowerCase(), key -> new ClassDocumentation(null, null, null, null, false));
                for(int i = 1; i < nameSplits.length - 2; i++) {
                    parent = parent.subClasses.computeIfAbsent(nameSplits[i].toLowerCase(), key -> new ClassDocumentation(null, null, null, null, false));
                }
                if(parent.subClasses.containsKey(className.toLowerCase()))
                    classDoc.subClasses.putAll(parent.subClasses.get(className.toLowerCase()).subClasses);
                parent.subClasses.put(className.toLowerCase(), classDoc);

                //store for later subclass indexing
                String actualClassName = nameSplits[nameSplits.length - 2].toLowerCase();
                ClassDocumentation subClassesNode = docs.computeIfAbsent(SUBCLASSES_MAP_KEY, key -> new ClassDocumentation(null, null, null, null, false));
                ClassDocumentation subClassElem = subClassesNode.subClasses.get(actualClassName);
                if(subClassElem != null && subClassElem.classSig != null)
                    subClassesNode.subClasses.put(actualClassName, new ClassDocumentation(null, null, null, null, false));
                else if(subClassElem == null)
                    subClassesNode.subClasses.put(actualClassName, classDoc);
            } else {
                //top-level class, store in map's root
                ClassDocumentation current = docs.get(className.toLowerCase());
                if(current != null && current.classSig != null)
                    throw new RuntimeException(String.format("Got a class-name conflict with classes %s.%s AND %s.%s",
                            classDoc.pack, classDoc.className, current.pack, current.className));
                if(current != null)
                    classDoc.subClasses.putAll(current.subClasses);
                docs.put(className.toLowerCase(), classDoc);
            }
        } catch (final IOException | NullPointerException ex) {
            JDocUtil.LOG.error("Got excaption for element {}", fullName, ex);
        } finally {
            try {
                inputStream.close();
            } catch(final IOException e) {
                JDocUtil.LOG.error("Error closing inputstream", e);
            }
        }
    }

    private static Map<String, String> getInheritedMethods(Element summaryAnchor) {
        Map<String, String> inherited = new HashMap<>();
        if(summaryAnchor == null)
            return inherited;
        summaryAnchor = summaryAnchor.parent();
        Elements inheritAnchors = summaryAnchor.select("a[name^=\"methods.inherited.from.class\"]");
        for(Element inheritAnchor : inheritAnchors) {
            if(inheritAnchor.siblingElements().size() != 2)
                throw new RuntimeException("Got unexpected html while parsing inherited methods from class " + inheritAnchor.attr("name"));
            Element next = inheritAnchor.nextElementSibling();
            if(!next.tagName().equals("h3"))
                throw new RuntimeException("Got unexpected html while parsing inherited methods from class " + inheritAnchor.attr("name"));
            Element sub = next.children().last();
            if(sub == null || !sub.tagName().equals("a"))
                continue;
            String parent = sub.text().toLowerCase();
            next = next.nextElementSibling();
            if(!next.tagName().equals("code"))
                throw new RuntimeException("Got unexpected html while parsing inherited methods from class " + inheritAnchor.attr("name"));
            for(sub = next.children().first(); sub != null; sub = sub.nextElementSibling()) {
                if(sub.tagName().equals("a")) {
                    inherited.putIfAbsent(sub.text().toLowerCase(), parent);
                }
            }
        }
        return inherited;
    }

    private static List<DocBlock> getDocBlock(String jdocBase, Element elem, ClassDocumentation reference) {
        if(elem != null) {
            String baseLink = JDocUtil.getLink(jdocBase, reference);
            List<DocBlock> blocks = new ArrayList<>(10);
            String hashLink = null;
            for(elem = elem.nextElementSibling(); elem != null; elem = elem.nextElementSibling()) {
                if(elem.tagName().equals("a")) {
                    hashLink = '#' + elem.attr("name");
                } else if(elem.tagName().equals("ul")) {
                    Element tmp = elem.getElementsByTag("h4").first();
                    String title = JDocUtil.fixSpaces(tmp.text().trim());
                    String description = "", signature = "";
                    OrderedMap<String, List<String>> fields = new ListOrderedMap<>();
                    for(;tmp != null; tmp = tmp.nextElementSibling()) {
                        if(tmp.tagName().equals("pre")) {
                            //contains full signature
                            signature = JDocUtil.fixSpaces(tmp.text().trim());
                        } else if(tmp.tagName().equals("div") && tmp.className().equals("block")) {
                            //main block of content (description or deprecation)
                            Element deprecationElem = tmp.getElementsByClass("deprecationComment").first();
                            if(deprecationElem != null) {
                                //deprecation block
                                fields.put("Deprecated:", Collections.singletonList(JDocUtil.formatText(deprecationElem.html(), baseLink)));
                            } else {
                                //description block
                                description = JDocUtil.formatText(tmp.html(), baseLink);
                            }
                        } else if(tmp.tagName().equals("dl")) {
                            //a field
                            String fieldName = null;
                            List<String> fieldValues = new ArrayList<>();
                            for(Element element : tmp.children()) {
                                if(element.tagName().equals("dt")) {
                                    if(fieldName != null) {
                                        fields.put(fieldName, fieldValues);
                                        fieldValues = new ArrayList<>();
                                    }
                                    fieldName = JDocUtil.fixSpaces(element.text().trim());
                                } else if(element.tagName().equals("dd")) {
                                    fieldValues.add(JDocUtil.formatText(element.html(), baseLink));
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

    static class ClassDocumentation implements Documentation {
        final String                                pack;
        final String                                className;
        final String                                classSig;
        final String                                classDesc;
        final boolean                               isEnum;
        final Map<String, Set<MethodDocumentation>> methodDocs = new HashMap<>();
        final Map<String, ClassDocumentation>       subClasses = new HashMap<>();
        final Map<String, ValueDocumentation>       classValues = new HashMap<>();
        final Map<String, String>                   inheritedMethods = new HashMap<>();

        private ClassDocumentation(String pack, String className, String classSig, String classDesc, boolean isEnum) {
            this.pack = pack;
            this.className = className;
            this.classSig = classSig;
            this.classDesc = classDesc;
            this.isEnum = isEnum;
        }

        @Override
        public String getTitle() {
            return getShortTitle();
        }

        @Override
        public String getShortTitle() {
            return classSig;
        }

        @Override
        public String getUrl(String jdocBase) {
            return JDocUtil.getLink(jdocBase, this);
        }

        @Override
        public String getContent() {
            return classDesc;
        }

        @Override
        public Map<String, List<String>> getFields() {
            if(!isEnum)
                return null;
            Map<String, List<String>> fields = new HashMap<>();
            fields.put("Values:", classValues.values().stream().map(valueDoc -> valueDoc.name).collect(Collectors.toList()));
            return fields;
        }
    }

    static class MethodDocumentation implements Documentation {
        final ClassDocumentation                parent;
        final List<MethodAnnotation>            methodAnnos;
        final String                            returnType;
        final String                            functionName;
        final String                            parameters;
        final String                            functionSig;
        final List<String>                      argTypes;
        final String                            hashLink;
        final String                            desc;
        final OrderedMap<String, List<String>>  fields;

        private MethodDocumentation(ClassDocumentation parent, String functionSig, final String hashLink, final String desc, final OrderedMap<String, List<String>> fields) {
            functionSig = JDocUtil.fixSignature(functionSig);
            Matcher methodMatcher = METHOD_PATTERN.matcher(functionSig);
            if(!methodMatcher.find()) {
                System.out.println('"' + functionSig + '"');
                throw new RuntimeException("Got method with no proper method signature: " + functionSig);
            }
            //check for documented annotations of method
            this.methodAnnos = new ArrayList<>();
            Matcher annoGroupMatcher = ANNOTATION_PATTERN.matcher(functionSig);
            if(annoGroupMatcher.find()) {
                Matcher annoMatcher = ANNOTATION_PARTS.matcher(annoGroupMatcher.group(1));
                while(annoMatcher.find()) {
                    this.methodAnnos.add(new MethodAnnotation(annoMatcher.group(1), annoMatcher.group(2)));
                }
            }
            this.parent = parent;
            this.returnType = methodMatcher.group(1);
            this.functionName = methodMatcher.group(2);
            this.parameters = methodMatcher.group(3);
            this.functionSig = methodMatcher.group();
            this.hashLink = hashLink;
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

        boolean matches(String input, boolean fuzzy) {
            final Matcher matcher = METHOD_PATTERN.matcher("ff " + input);
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

        @Override
        public String getShortTitle() {
            return parent.className + '#' + functionName + '(' + parameters + ") : " + returnType;
        }

        @Override
        public String getTitle() {
            return getAnnoPrefix() + "\n" + getShortTitle();
        }

        @Override
        public String getUrl(String jdocBase) {
            return JDocUtil.getLink(jdocBase, parent) + hashLink;
        }

        @Override
        public String getContent() {
            return desc;
        }

        @Override
        public Map<String, List<String>> getFields() {
            return fields;
        }

        private String getAnnoPrefix() {
            if(this.methodAnnos.isEmpty())
                return "";
            boolean deprecated = false;
            String deprecatedSince = null;
            StringBuilder builder = new StringBuilder();
            for(MethodAnnotation methodAnno : this.methodAnnos) {
                switch(methodAnno.name) {
                    case "Deprecated":
                        deprecated = true;
                        break;
                    case "DeprecatedSince":
                        deprecatedSince = methodAnno.args.substring(2, methodAnno.args.length() - 2);
                        break;
                    default:
                        builder.append('@').append(methodAnno.toString()).append(' ');
                        break;
                }
            }

            if(deprecated || deprecatedSince != null) {
                StringBuilder tmp = new StringBuilder("@Deprecated");
                if(deprecatedSince != null)
                    tmp.append("(Since ").append(deprecatedSince).append(") ");
                builder = tmp.append(builder);
            }

            return builder.substring(0, builder.length() - 1);
        }

        private static class MethodAnnotation {
            private final String name;
            private final String args;

            private MethodAnnotation(String name, String args) {
                this.name = name;
                this.args = args;
            }

            @Override
            public String toString() {
                return name + (args == null ? "" : args);
            }
        }
    }

    static class ValueDocumentation implements Documentation {
        final ClassDocumentation parent;
        final String name;
        final String hashLink;
        final String sig;
        final String desc;

        private ValueDocumentation(ClassDocumentation parent, String name, String hashLink, String sig, String desc) {
            this.parent = parent;
            this.name = name;
            this.hashLink = hashLink;
            this.sig = sig;
            this.desc = desc;
        }

        @Override
        public String getTitle() {
            return parent.isEnum ? getShortTitle() : parent.className + " - " + this.sig;
        }

        @Override
        public String getShortTitle() {
            return parent.className + '.' + this.name;
        }

        @Override
        public String getUrl(String jDocBase) {
            return JDocUtil.getLink(jDocBase, parent) + hashLink;
        }

        @Override
        public String getContent() {
            return desc;
        }
    }
}
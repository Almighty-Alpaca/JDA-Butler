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

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class JDoc {
    private static final Map<String, JDocParser.ClassDocumentation> docs = new HashMap<>();
    private static final Map<String, String> javaJavaDocs = new HashMap<>();

    public static List<Documentation> get(final String name) {
        final String[] split = name.toLowerCase().split("[#.]");
        JDocParser.ClassDocumentation classDoc;
        synchronized(docs) {
            if (!docs.containsKey(split[0]))
                return Collections.emptyList();
            classDoc = docs.get(split[0]);
        }
        for(int i=1; i < split.length - 1; i++) {
            if(!classDoc.subClasses.containsKey(split[i]))
                return Collections.emptyList();
            classDoc = classDoc.subClasses.get(split[i]);
        }

        String searchObj = split[split.length - 1];
        if(split.length == 1 || classDoc.subClasses.containsKey(searchObj)) {
            if(split.length > 1)
                classDoc = classDoc.subClasses.get(searchObj);
            return Collections.singletonList(classDoc);
        } else if(classDoc.classValues.containsKey(searchObj)) {
            return Collections.singletonList(classDoc.classValues.get(searchObj));
        } else {
            boolean fuzzy = false;
            String fixedSearchObj = searchObj;
            if(fixedSearchObj.charAt(fixedSearchObj.length() - 1) != ')') {
                fixedSearchObj += "()";
                fuzzy = true;
            }
            String[] methodParts = fixedSearchObj.split("[()]");
            String methodName = methodParts[0];
            if(classDoc.methodDocs.containsKey(methodName.toLowerCase())) {
                return getMethodDocs(classDoc, methodName, fixedSearchObj, fuzzy);
            } else if(classDoc.inheritedMethods.containsKey(methodName.toLowerCase())) {
                return get(classDoc.inheritedMethods.get(methodName.toLowerCase()) + '.' + searchObj);
            }
            return Collections.emptyList();
        }
    }

    public static List<Documentation> getJava(final String name) {
        final String[] noArgNames = name.toLowerCase().split("\\(")[0].split("[#.]");
        String className = String.join(".", Arrays.copyOf(noArgNames, noArgNames.length - 1));
        String urlPath;
        synchronized(javaJavaDocs) {
            urlPath = javaJavaDocs.get(name.toLowerCase());
            if(urlPath == null)
                urlPath = javaJavaDocs.get(className);
            else
                className = name.toLowerCase();
            if (urlPath == null)
                return Collections.emptyList();
        }

        Map<String, JDocParser.ClassDocumentation> resultMap = new HashMap<>();
        InputStream is = null;
        try {
            Response res = Bot.httpClient.newCall(new Request.Builder().url(JDocUtil.JAVA_JDOCS_PREFIX+urlPath).get().build()).execute();
            if(!res.isSuccessful()) {
                JDocUtil.LOG.warn("OkHttp returned failure for java8 index: "+res.code());
                return Collections.emptyList();
            }
            is = res.body().byteStream();
            JDocParser.parse(JDocUtil.JAVA_JDOCS_PREFIX, urlPath, is, resultMap);
        } catch(Exception e) {
            JDocUtil.LOG.error("Error parsing java javadocs for {}", name, e);
        } finally {
            if(is != null)
                try { is.close(); } catch(Exception ignored) {}
        }

        if(!resultMap.containsKey(className)) {
            JDocUtil.LOG.warn("Parser didn't return wanted docs");
            return Collections.emptyList();
        }

        JDocParser.ClassDocumentation doc = resultMap.get(className);

        if(noArgNames.length == 1 || className.equalsIgnoreCase(name))
            return Collections.singletonList(doc);

        String searchObj = name.toLowerCase().substring(className.length() + 1);//class name + seperator dot
        if(doc.classValues.containsKey(searchObj)) {
            return Collections.singletonList(doc.classValues.get(searchObj));
        } else {
            boolean fuzzy = false;
            String fixedSearchObj = searchObj;
            if(fixedSearchObj.charAt(fixedSearchObj.length() - 1) != ')') {
                fixedSearchObj += "()";
                fuzzy = true;
            }
            String[] methodParts = fixedSearchObj.split("[()]");
            String methodName = methodParts[0];
            if(doc.methodDocs.containsKey(methodName.toLowerCase())) {
                return getMethodDocs(doc, methodName, fixedSearchObj, fuzzy);
            } else if(doc.inheritedMethods.containsKey(methodName.toLowerCase())) {
                return getJava(doc.inheritedMethods.get(methodName.toLowerCase()) + '.' + searchObj);
            }
            return Collections.emptyList();
        }
    }

    /**
     * Searches the whole JavaDocs based on input string and options
     *
     * @param input The text to search for.
     * @param options Options refining the search. Valid options are:
     *                <ul>
     *                <li>cs - makes matching case-sensitive</li>
     *                <li>f - only methods are searched. Can't be used together with other type-specific filters.</li>
     *                <li>c - only classes are searched. Can't be used together with other type-specific filters.</li>
     *                <li>var - only values are searched. Can't be used together with other type-specific filters.</li>
     *                </ul>
     * @return Pairs of the form: Text-representation - Documentation
     * @throws PatternSyntaxException if regex was used and the regex is not valid
     */
    public static Set<Pair<String, ? extends Documentation>> search(String input, String... options) throws PatternSyntaxException {
        Set<String> opts = Arrays.stream(options).map(String::toLowerCase).collect(Collectors.toSet());
        final boolean isCaseSensitive = opts.contains("cs");
        String key = input.toLowerCase();
        if(opts.contains("f")) {
            return docs.values().stream()
                    .flatMap(cls -> cls.methodDocs.entrySet().stream()
                            .filter(mds -> mds.getKey().contains(key))
                            .map(Map.Entry::getValue)
                            .flatMap(Collection::stream)
                    )
                    .filter(md -> !isCaseSensitive || md.functionName.contains(input))
                    .map(md -> Pair.of(md.parent.className+" "+md.functionSig, md))
                    .collect(Collectors.toSet());
        } else if(opts.contains("c")) {
            return docs.values().stream()
                    .filter(cls -> isCaseSensitive ? cls.className.contains(input) : cls.className.toLowerCase().contains(key))
                    .map(cls -> Pair.of("Class "+cls.className, cls))
                    .collect(Collectors.toSet());
        } else if(opts.contains("var")) {
            return docs.values().stream()
                    .flatMap(cls -> cls.classValues.entrySet().stream()
                            .filter(val -> val.getKey().contains(key))
                            .map(Map.Entry::getValue)
                    )
                    .filter(val -> !isCaseSensitive || val.name.contains(input))
                    .map(val -> Pair.of(val.parent.className + " "+val.sig, val))
                    .collect(Collectors.toSet());
        } else {
            //search all categories
            Set<Pair<String, ? extends Documentation>> results = new HashSet<>();
            for(JDocParser.ClassDocumentation classDoc : docs.values()) {
                if(isCaseSensitive ? classDoc.className.contains(input) : classDoc.className.toLowerCase().contains(key))
                    results.add(Pair.of("Class " + classDoc.className, classDoc));
                for(Set<JDocParser.MethodDocumentation> mdcs : classDoc.methodDocs.values()) {
                    for(JDocParser.MethodDocumentation mdc : mdcs) {
                        if(isCaseSensitive ? mdc.functionName.contains(input) : mdc.functionName.toLowerCase().contains(key))
                            results.add(Pair.of(mdc.parent.className+" "+mdc.functionSig, mdc));
                    }
                }
                for(JDocParser.ValueDocumentation valueDoc : classDoc.classValues.values()) {
                    if(isCaseSensitive ? valueDoc.name.contains(input) : valueDoc.name.toLowerCase().contains(key))
                        results.add(Pair.of(valueDoc.parent.className+" "+valueDoc.sig, valueDoc));
                }
            }
            return results;
        }
    }

    private static List<Documentation> getMethodDocs(JDocParser.ClassDocumentation classDoc, String methodName, String methodSig, boolean isFuzzy) {
        List<JDocParser.MethodDocumentation> docs = classDoc.methodDocs.get(methodName.toLowerCase())
                .stream()
                .sorted(Comparator.comparingInt(m -> m.argTypes.size()))
                .collect(Collectors.toList());
        List<JDocParser.MethodDocumentation> filteredDocs = docs.parallelStream()
                .filter(doc -> doc.matches(methodSig, isFuzzy))
                .collect(Collectors.toList());
        switch (filteredDocs.size())
        {
            case 1:
                return Collections.singletonList(filteredDocs.get(0));
            case 0:
                return Collections.unmodifiableList(docs);
            default:
                return Collections.unmodifiableList(filteredDocs);
        }
    }

    public static void init() {
        if (!docs.isEmpty())
            return;
        JDocUtil.LOG.info("Initializing Docs...");
        download();
        fetch();
        JDocUtil.LOG.debug("JDA-Docs initialized, Fetching Java8 class indexes");
        fetchJavaClassIndexes();
        JDocUtil.LOG.info("Docs initialized");
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
            JDocUtil.LOG.error("Error re-fetching jdocs", e);
        }
    }

    private static void fetch() {
        Map<String, JDocParser.ClassDocumentation> newDocs = JDocParser.parse();
        synchronized(docs) {
            docs.putAll(newDocs);
        }
    }

    private static void download() {
        try
        {
            JenkinsBuild lastBuild = JenkinsApi.JDA_JENKINS.getLastSuccessfulBuild();
            if(lastBuild != null)
            {
                JDocUtil.LOG.debug("Downloading JDA docs...");
                ResponseBody body = null;
                try
                {
                    String artifactUrl = lastBuild.artifacts.get("JDA-javadoc").getLink();
                    Response res = Bot.httpClient.newCall(new Request.Builder().url(artifactUrl).get().build()).execute();
                    if(!res.isSuccessful())
                    {
                        JDocUtil.LOG.warn("OkHttp returned failure for " + artifactUrl);
                        return;
                    }
                    body = res.body();
                    final InputStream is = body.byteStream();
                    Files.copy(is, JDocUtil.LOCAL_DOC_PATH, StandardCopyOption.REPLACE_EXISTING);
                    is.close();
                    JDocUtil.LOG.debug("Done downloading JDA docs");
                }
                catch(Exception e)
                {
                    JDocUtil.LOG.error("Error downloading jdoc jar", e);
                }
                finally
                {
                    if(body != null)
                        body.close();
                }
            }
            else
            {
                JDocUtil.LOG.warn("There was no Jenkins build?! Skipping download");
            }
        }
        catch(IOException ex)
        {
            JDocUtil.LOG.warn("Could not contact Jenkins, skipping download");
        }
    }

    private static void fetchJavaClassIndexes() {
        try {
            Response res = Bot.httpClient.newCall(new Request.Builder().url(JDocUtil.JAVA_JDOCS_CLASS_INDEX).get().build()).execute();
            if(!res.isSuccessful()) {
                JDocUtil.LOG.warn("OkHttp returned failure for java8 index: "+res.code());
                return;
            }
            ResponseBody body = res.body();
            Document docBody = Jsoup.parse(body.byteStream(), "UTF-8", JDocUtil.JAVA_JDOCS_PREFIX);
            docBody.getElementsByClass("indexContainer").first().child(0).children().forEach(child -> {
                Element link = child.child(0);
                if(link.tagName().equals("a") && link.attr("href").startsWith("java/")) {
                    javaJavaDocs.put(link.text().toLowerCase(), link.attr("href"));
                }
            });
        } catch(Exception e) {
            JDocUtil.LOG.error("Failed fetching the j8 class index", e);
        }
    }
}

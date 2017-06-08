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
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class JDoc {
    private static final Map<String, JDocParser.ClassDocumentation> docs = new HashMap<>();

    public static List<Documentation> get(final String name) {
        final String[] split = name.toLowerCase().split("[#\\.]");
        JDocParser.ClassDocumentation classDoc;
        synchronized(docs) {
            if (!docs.containsKey(split[0]))
                return new ArrayList<>(0);
            classDoc = docs.get(split[0]);
        }
        for(int i=1; i < split.length - 1; i++) {
            if(!classDoc.subClasses.containsKey(split[i]))
                return new ArrayList<>(0);
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
            String[] methodParts = fixedSearchObj.split("[\\(\\)]");
            String methodName = methodParts[0];
            if(classDoc.methodDocs.containsKey(methodName.toLowerCase())) {
                return getMethodDocs(classDoc, methodName, fixedSearchObj, fuzzy);
            } else if(classDoc.inheritedMethods.containsKey(methodName.toLowerCase())) {
                return get(classDoc.inheritedMethods.get(methodName.toLowerCase()) + '.' + searchObj);
            }
            return new ArrayList<>(0);
        }
    }

    /**
     * Searches the whole JavaDocs based on input string and options
     *
     * @param input The text to search for.
     * @param options Options refining the search. Valid options are:
     *                <ul>
     *                <li>re or r - input treated as regex</li>
     *                <li>cs - makes matching case-sensitive</li>
     *                <li>m or f - only methods are searched. Can't be used together with c or v.</li>
     *                <li>c - only classes are searched. Can't be used together with m/f or v.</li>
     *                <li>v - only values are searched. Can't be used together with m/f or c.</li>
     *                </ul>
     *                If no filter-option is used, Search is similar to {@link #get(String)} and supports dots and # to split classes and methods/values (dots need to be escaped when using regex)
     * @return Pairs of the form: Text-representation - Documentation
     * @throws PatternSyntaxException if regex was used and the regex is not valid
     */
    public static Set<Pair<String, ? extends Documentation>> search(String input, String... options) throws PatternSyntaxException {
        Set<String> opts = Arrays.stream(options).map(String::toLowerCase).collect(Collectors.toSet());
        final boolean usesRegex = opts.contains("r") || opts.contains("re");
        final boolean isCaseSensitive = opts.contains("cs");
        if(opts.contains("m") || opts.contains("f")) {
            final Pattern regexPattern = getPattern(input, usesRegex, isCaseSensitive);
            return docs.values().stream()
                    .flatMap(cls -> cls.methodDocs.entrySet().stream()
                            .filter(md -> regexPattern.matcher(md.getKey()).find())
                            .map(Map.Entry::getValue)
                            .flatMap(Collection::stream)
                    )
                    .filter(md -> !isCaseSensitive || regexPattern.matcher(md.functionName).find())
                    .map(md -> Pair.of(md.parent.className+" "+md.functionSig, md))
                    .collect(Collectors.toSet());
        } else if(opts.contains("c")) {
            final Pattern regexPattern = getPattern(input, usesRegex, isCaseSensitive);
            return docs.values().stream()
                    .filter(clss -> regexPattern.matcher(clss.className).find())
                    .map(cls -> Pair.of("Class "+cls.className, cls))
                    .collect(Collectors.toSet());
        } else if(opts.contains("v")) {
            final Pattern regexPattern = getPattern(input, usesRegex, isCaseSensitive);
            return docs.values().stream()
                    .flatMap(cls -> cls.classValues.entrySet().stream()
                            .filter(val -> regexPattern.matcher(val.getKey()).find())
                            .map(Map.Entry::getValue)
                    )
                    .filter(val -> !isCaseSensitive || regexPattern.matcher(val.name).find())
                    .map(val -> Pair.of(val.parent.className + " "+val.sig, val))
                    .collect(Collectors.toSet());
        } else {
            String[] split = input.split(usesRegex ? "(?:\\\\{1,2}\\.|#)" : "[\\.#]");
            if(split.length == 1) {
                Pattern pattern = getPattern(split[0], usesRegex, isCaseSensitive);
                //search all categories
                Set<Pair<String, ? extends Documentation>> results = new HashSet<>();
                for(JDocParser.ClassDocumentation classDocumentation : docs.values()) {
                    if(pattern.matcher(classDocumentation.className).find())
                        results.add(Pair.of("Class " + classDocumentation.className, classDocumentation));
                    for(Set<JDocParser.MethodDocumentation> mdcs : classDocumentation.methodDocs.values()) {
                        for(JDocParser.MethodDocumentation mdc : mdcs) {
                            if(pattern.matcher(mdc.functionName).find())
                                results.add(Pair.of(mdc.parent.className+" "+mdc.functionSig, mdc));
                        }
                    }
                    for(JDocParser.ValueDocumentation valueDocumentation : classDocumentation.classValues.values()) {
                        if(pattern.matcher(valueDocumentation.name).find())
                            results.add(Pair.of(valueDocumentation.parent.className+" "+valueDocumentation.sig, valueDocumentation));
                    }
                }
                return results;
            } else {
                StringBuilder sb = new StringBuilder(split[0]);
                for(int i = 1; i < split.length -1; i++) {
                    sb.append("\\.").append(split[i]);
                }
                Pattern classPattern = getPattern(sb.toString(), usesRegex, isCaseSensitive);
                Pattern valuePattern = getPattern(split[split.length - 1], usesRegex, isCaseSensitive);

                Set<Pair<String, ? extends Documentation>> results = new HashSet<>();
                for(JDocParser.ClassDocumentation clsdc : docs.values()) {
                    if(classPattern.matcher(clsdc.className).find()) {
                        for(JDocParser.ClassDocumentation subcls : clsdc.subClasses.values()) {
                            if(valuePattern.matcher(subcls.className.substring(clsdc.className.length() + 1)).find())
                                results.add(Pair.of("Class " + subcls.className, subcls));
                        }
                        for(JDocParser.ValueDocumentation val : clsdc.classValues.values()) {
                            if(valuePattern.matcher(val.name).find())
                                results.add(Pair.of(val.parent.className+" "+val.sig, val));
                        }
                        for(Set<JDocParser.MethodDocumentation> mdcs : clsdc.methodDocs.values()) {
                            for(JDocParser.MethodDocumentation mdc : mdcs) {
                                if(valuePattern.matcher(mdc.functionName).find())
                                    results.add(Pair.of(mdc.parent.className+" "+mdc.functionSig, mdc));
                            }
                        }
                    }
                }
                return results;
            }
        }
    }

    private static Pattern getPattern(String input, boolean isRegex, boolean caseSensitive) {
        if(!caseSensitive)
            return Pattern.compile(isRegex ? input : Pattern.quote(input.toLowerCase()), Pattern.CASE_INSENSITIVE);
        return Pattern.compile(isRegex ? input : Pattern.quote(input.toLowerCase()));
    }

    private static List<Documentation> getMethodDocs(JDocParser.ClassDocumentation classDoc, String methodName, String methodSig, boolean isFuzzy) {
        List<JDocParser.MethodDocumentation> docs = classDoc.methodDocs.get(methodName.toLowerCase())
                .stream()
                .sorted(Comparator.comparingInt(m -> m.argTypes.size()))
                .collect(Collectors.toList());
        List<JDocParser.MethodDocumentation> filteredDocs = docs.parallelStream()
                .filter(doc -> doc.matches(methodSig, isFuzzy))
                .collect(Collectors.toList());
        if(filteredDocs.size() == 1) {
            return Collections.singletonList(filteredDocs.get(0));
        } else if(filteredDocs.size() == 0) {
            return Collections.unmodifiableList(docs);
        } else {
            return Collections.unmodifiableList(filteredDocs);
        }
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

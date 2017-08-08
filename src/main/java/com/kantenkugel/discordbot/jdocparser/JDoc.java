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

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
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
        JenkinsBuild lastBuild = JenkinsApi.getLastSuccessfulBuild();
        if(lastBuild != null)
        {
            JDocUtil.LOG.info("Downloading JDA docs...");
            try {
                final URL artifactUrl = new URL(lastBuild.artifacts.get("JDA-javadoc").getLink());
                final URLConnection connection = artifactUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                final InputStream is = connection.getInputStream();
                Files.copy(is, JDocUtil.LOCAL_DOC_PATH, StandardCopyOption.REPLACE_EXISTING);
                is.close();
                JDocUtil.LOG.info("Done downloading JDA docs");
            } catch(Exception e) {
                JDocUtil.LOG.log(e);
            }
        }
    }
}

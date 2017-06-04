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

import com.almightyalpaca.discord.jdabutler.JDAUtil;
import net.dv8tion.jda.core.utils.SimpleLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JDocUtil {
    static final SimpleLog LOG = SimpleLog.getLog("JDoc");

    static final Path LOCAL_DOC_PATH = Paths.get("jda-docs.jar");

    static final String JENKINSBASE = "http://" + JDAUtil.JENKINS_BASE.get() + ":8080/job/JDA/lastSuccessfulBuild/";
    static final String JDOCBASE = JENKINSBASE + "javadoc/";
    static final String ARTIFACT_URL = JENKINSBASE + "api/json?tree=artifacts[*]";

    static final String JDA_CODE_BASE = "net/dv8tion/jda";

    private static final Pattern LINK_PATTERN = Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>");
    private static final Pattern CODE_PATTERN = Pattern.compile("<code>(.*?)</code>");

    static String formatText(String docs, String currentUrl) {
        docs = fixSpaces(docs);
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

    static String fixSpaces(String input) {
        return input == null ? null : input.replaceAll("\\h", " ");
    }

    static String getLink(JDocParser.ClassDocumentation doc) {
        return getLink(doc.pack, doc.className);
    }

    static String getLink(String classPackage, String className) {
        return JDOCBASE + classPackage.replace(".", "/") + '/' + className + ".html";
    }

    static String resolveLink(String href, String relativeTo) {
        try {
            URL base = new URL(relativeTo);
            URL result = new URL(base, href);
            return result.toString();
        } catch(MalformedURLException e) {
            LOG.log(e);
        }
        return null;
    }


}

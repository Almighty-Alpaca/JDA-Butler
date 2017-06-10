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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JDocUtil {
    static final SimpleLog LOG = SimpleLog.getLog("JDoc");

    static final Path LOCAL_DOC_PATH = Paths.get("jda-docs.jar");

    static final String JENKINSBASE = "http://" + JDAUtil.JENKINS_BASE.get() + ":8080/job/JDA/lastSuccessfulBuild/";
    public static final String JDOCBASE = JENKINSBASE + "javadoc/";
    static final String ARTIFACT_URL = JENKINSBASE + "api/json?tree=artifacts[*]";

    static final String JDA_CODE_BASE = "net/dv8tion/jda";

    //1: outer "`", 2: href, 3: inner "`", 4: text
    private static final Pattern LINK_PATTERN = Pattern.compile("(`?)<a\\b[^>]*href=\"([^\"]+)\"[^>]*>(`?)(.*?)\\3</a>\\1", Pattern.DOTALL);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("<pre>\\s*<code>(.*?)</code>\\s*</pre>", Pattern.DOTALL);
    private static final Pattern CODE_PATTERN = Pattern.compile("<code>(.*?)</code>", Pattern.DOTALL);

    static String formatText(String docs, String currentUrl) {
        //fix all spaces to be " "
        docs = fixSpaces(docs);

        //remove new-lines (only use <br>)
        //docs = docs.replace("\n", " ");

        //basic formatting
        docs = docs.replaceAll("</?b>", "**").replaceAll("</?i>", "*");
        docs = docs.replaceAll("<br\\s?/?>", "\n");
        docs = docs.replaceAll("<h(\\d)>(.*?)</h\\1>", "\n\n**$2**\n\n");
        docs = docs.replaceAll("<p>(.*?)</p>", "\n\n$1\n\n");

        //code
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(docs);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(sb, "```java\n" + matcher.group(1).replaceAll("(?=[\n|\\h])\\h(?=[^\n])", "\u00A0").trim() + "\n```\n");
        }
        matcher.appendTail(sb);
        docs = sb.toString();
        docs = CODE_PATTERN.matcher(docs).replaceAll("`$1`");

        //links
        matcher = LINK_PATTERN.matcher(docs);
        sb = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(sb, '[' +
                    ((!matcher.group(1).isEmpty() || !matcher.group(3).isEmpty()) ? "***" : "") +
                    fixSignature(matcher.group(4).replace("*", "")) +
                    ((!matcher.group(1).isEmpty() || !matcher.group(3).isEmpty()) ? "***" : "") +
                    "](" + resolveLink(matcher.group(2), currentUrl) + ')'
            );
        }
        matcher.appendTail(sb);
        docs = sb.toString();

        //cut remaining html tags
        docs = docs.replaceAll("<[^>]+>", "");
        docs = docs.replace("&lt;", "<").replace("&gt;", ">");

        //space and newline trimming cleanup
        docs = docs.replaceAll("[ ]{2,}", " ");
        docs = docs.replaceAll("\n\\h+\n", "\n\n");
        //fixes stranded words and line breaks cuz of link/html-tags, but fucks up code-blocks
        docs = docs.replaceAll("\n[ ](?![ ])", " ").replaceAll("[ ](?![ ])\n", " ");
        docs = docs.replaceAll("\n{3,}", "\n\n");
        docs = docs.trim();

        return docs;
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

    static String fixSignature(String sig) {
        return sig.replaceAll("\\b(?:[a-z]+\\.)+([A-Z])", "$1").replaceAll("\\s{2,}", " ");
    }

}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JDocUtil {
    static final Logger LOG = LoggerFactory.getLogger("JDoc");

    static final Path LOCAL_DOC_PATH = Paths.get("jda-docs.jar");

    public static final String JAVA_JDOCS_PREFIX = "https://docs.oracle.com/javase/8/docs/api/";
    static final String JAVA_JDOCS_CLASS_INDEX = JAVA_JDOCS_PREFIX + "allclasses-noframe.html";

    public static final String JDOCBASE = JenkinsApi.JDA_JENKINS.getLastSuccessfulBuildUrl() + "javadoc/";

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
        sb = new StringBuffer();
        matcher = CODE_PATTERN.matcher(docs);
        while(matcher.find()) {
            StringBuffer sb2 = new StringBuffer("`");
            Matcher codeLinkMatcher = LINK_PATTERN.matcher(matcher.group(1));
            while(codeLinkMatcher.find()) {
                codeLinkMatcher.appendReplacement(sb2, codeLinkMatcher.group(4));
            }
            codeLinkMatcher.appendTail(sb2);
            sb2.append('`');
            matcher.appendReplacement(sb, sb2.toString().replace("$", "\\$"));
        }
        matcher.appendTail(sb);
        docs = sb.toString();

        //links
        matcher = LINK_PATTERN.matcher(docs);
        sb = new StringBuffer();
        while(matcher.find()) {
            matcher.appendReplacement(sb,
                    ((!matcher.group(1).isEmpty() || !matcher.group(3).isEmpty()) ? "***" : "") +
                    '[' +
                    fixSignature(matcher.group(4).replace("*", "")) +
                    ']' +
                    '(' + resolveLink(matcher.group(2), currentUrl) + ')' +
                    ((!matcher.group(1).isEmpty() || !matcher.group(3).isEmpty()) ? "***" : "")
            );
        }
        matcher.appendTail(sb);
        docs = sb.toString();

        //cut remaining html tags
        docs = docs.replaceAll("<[^>]+>", "");
        docs = docs.replace("&lt;", "<").replace("&gt;", ">");
        docs = docs.replace("&nbsp;", " ");

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

    static String getLink(String jdocBase, JDocParser.ClassDocumentation doc) {
        return getLink(jdocBase, doc.pack, doc.className);
    }

    static String getLink(String jdocBase, String classPackage, String className) {
        return jdocBase + classPackage.replace(".", "/") + '/' + className + ".html";
    }

    static String resolveLink(String href, String relativeTo) {
        try {
            URL base = new URL(relativeTo);
            URL result = new URL(base, href);
            return result.toString();
        } catch(MalformedURLException e) {
            LOG.error("Could not resolve relative link of jdoc", e);
        }
        return null;
    }

    static String fixSignature(String sig) {
        return sig.replace("\u200B", "").replaceAll("\\b(?:[a-z]+\\.)+([A-Z])", "$1").replaceAll("\\s{2,}", " ");
    }

}

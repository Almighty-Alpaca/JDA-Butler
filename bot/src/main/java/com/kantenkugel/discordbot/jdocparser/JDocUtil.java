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
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JDocUtil {
    static final Logger LOG = LoggerFactory.getLogger("JDoc");

    static final Path LOCAL_DOC_PATH = Paths.get("jda-docs.jar");

    public static final String JAVA_JDOCS_PREFIX = "https://docs.oracle.com/javase/8/docs/api/";
    static final String JAVA_JDOCS_CLASS_INDEX = JAVA_JDOCS_PREFIX + "allclasses-noframe.html";

    public static final String JDOCBASE = JenkinsApi.JDA_JENKINS.jenkinsBase + "javadoc/";

    static final String JDA_CODE_BASE = "net/dv8tion/jda";

    private static final Remark REMARK;

    private static final Pattern CODEBLOCK_PATTERN = Pattern.compile("(```java\n)(.*?)(```)", Pattern.DOTALL);

    static {
        Options remarkOptions = Options.github();
        remarkOptions.inlineLinks = true;
        remarkOptions.fencedCodeBlocksWidth = 3;
        REMARK = new Remark(remarkOptions);
    }

    static String formatText(String docs, String currentUrl) {
        String markdown = REMARK.convertFragment(fixSpaces(docs), currentUrl);

        //remove unnecessary carriage return chars
        markdown = markdown.replace("\r", "");

        //fix codeblocks
        markdown = markdown.replace("\n\n```", "\n\n```java");
        Matcher matcher = CODEBLOCK_PATTERN.matcher(markdown);
        if(matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            do {
                matcher.appendReplacement(buffer, matcher.group(1) + matcher.group(2).replaceAll("\n\\s", "\n") + matcher.group(3));
            } while(matcher.find());
            matcher.appendTail(buffer);
            markdown = buffer.toString();
        }

        //remove too many newlines (max 2)
        markdown = markdown.replaceAll("\n{3,}", "\n\n");

        return markdown;
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

    static String fixUrl(String url) {
        return url.replace(")", "%29"); //as markdown doesn't allow ')' in urls
    }

    static String fixSignature(String sig) {
        return sig.replace("\u200B", "").replaceAll("\\b(?:[a-z]+\\.)+([A-Z])", "$1").replaceAll("\\s{2,}", " ");
    }
}

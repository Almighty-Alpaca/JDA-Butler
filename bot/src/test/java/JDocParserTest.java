import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.jdocparser.Documentation;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jdocparser.JDocUtil;
import okhttp3.OkHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class JDocParserTest {

    @BeforeClass
    public static void init() {
        Bot.httpClient = new OkHttpClient.Builder().build();
        JDoc.init();
    }

    @Test
    public void getNonExistentClass() {
        List<Documentation> docs = JDoc.get("bla");
        assertEquals("Non-Existent class should not return a Documentation", 0, docs.size());

        docs = JDoc.get("user.bla");
        assertEquals("Non-Existent class (child) should not return a Documentation", 0, docs.size());
    }

    @Test
    public void getMethodWithReturnAndThrows() {
        List<Documentation> docs = JDoc.get("messagechannel.getiterablehistory");
        assertEquals("MessageChannel.getIterableHistory should be found", 1, docs.size());
        Documentation doc = docs.get(0);
        assertTrue("Embed should have Return field", doc.getFields().keySet().stream().anyMatch(f -> f.equals("Returns:")));
        assertTrue("Embed should have Throws field", doc.getFields().keySet().stream().anyMatch(f -> f.equals("Throws:")));
    }

    @Test
    public void getMethodWithParameter() {
        List<Documentation> docs = JDoc.get("jda.getUserById(long)");
        assertEquals("JDA.getUserById(long) should be found", 1, docs.size());
        Documentation doc = docs.get(0);
        assertTrue("Embed should have Parameters field", doc.getFields().keySet().stream().anyMatch(f -> f.equals("Parameters:")));
    }

    @Test
    public void getEnumClass() {
        List<Documentation> docs = JDoc.get("accounttype");
        assertEquals("AccountType should be found", 1, docs.size());
        Documentation doc = docs.get(0);
        assertTrue("Embed should have Throws field", doc.getFields().keySet().stream().anyMatch(f -> f.equals("Values:")));
    }

    @Test
    public void getInnerClass() {
        List<Documentation> docs = JDoc.get("messagebuilder.formatting");
        assertEquals("MessageBuilder.Formatting should be found", 1, docs.size());
    }

    @Test
    public void getValue() {
        List<Documentation> docs = JDoc.get("messageembed.TITLE_MAX_LENGTH");
        assertEquals("MessageEmbed.TITLE_MAX_LENGTH should be found", 1, docs.size());
    }

    @Test
    public void getInheritedMethod() {
        List<Documentation> docs = JDoc.get("user.getid");
        assertEquals("User.getId - inherited should be found", 1, docs.size());
    }

    @Test
    public void getFuzzyResult() {
        List<Documentation> docs = JDoc.get("restaction.queue");
        assertEquals("restaction.queue should find 3 contestants", 3, docs.size());
    }

    @Test
    public void checkURL() {
        List<Documentation> message = JDoc.get("Message");
        assertEquals("Message should be found as single result", 1, message.size());
        assertEquals("URL of Message docs mismatches",
                "https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Message.html",
                message.get(0).getUrl(JDocUtil.JDOCBASE)
        );
    }

    @Test
    public void javaJDocsWork() {
        List<Documentation> stringFormat = JDoc.getJava("String#format");
        assertEquals("String#format should be found in Java Jdocs", 2, stringFormat.size());
    }

    @Test
    public void javaJDocUrlCheck() {
        List<Documentation> stringClass = JDoc.getJava("String");
        assertEquals("String should be found in Java Jdocs", 1, stringClass.size());
        assertEquals("Url to String JDocs mismatches",
                "https://docs.oracle.com/javase/8/docs/api/java/lang/String.html",
                stringClass.get(0).getUrl(JDocUtil.JAVA_JDOCS_PREFIX));
    }

    @Test
    public void javaJdocNamesWithNumbersWork() {
        List<Documentation> graphics2D = JDoc.getJava("Graphics2D");
        assertEquals("Could not find Class Graphics2D in Java JDocs", 1, graphics2D.size());
    }
}

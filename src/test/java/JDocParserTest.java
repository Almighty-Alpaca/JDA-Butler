import com.kantenkugel.discordbot.jdocparser.Documentation;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class JDocParserTest {

    @BeforeClass
    public static void init() {
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
        List<Documentation> docs = JDoc.get("simplelog.name");
        assertEquals("SimpleLog.name should be found", 1, docs.size());
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
}

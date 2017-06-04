import com.kantenkugel.discordbot.jdocparser.JDoc;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JDocParserTest {

    @BeforeClass
    public static void init() {
        JDoc.init();
    }

    @Test
    public void getNonExistentClass() {
        Message msg = JDoc.get("bla");
        assertEquals("Non-Existent class should not generate Embeds", 0, msg.getEmbeds().size());
        assertEquals("Class not Found!", msg.getRawContent());

        msg = JDoc.get("user.bla");
        assertEquals("Non-Existent element should not generate Embeds", 0, msg.getEmbeds().size());
        assertEquals("Could not find search-query", msg.getRawContent());
    }

    @Test
    public void getMethodWithReturnAndThrows() {
        Message msg = JDoc.get("messagechannel.getiterablehistory");
        assertEquals("MessageChannel.getIterableHistory should be found and therefore generate embed", 1, msg.getEmbeds().size());
        MessageEmbed embed = msg.getEmbeds().get(0);
        assertTrue("Embed should have Return field", embed.getFields().stream().anyMatch(f -> f.getName().equals("Returns:")));
        assertTrue("Embed should have Throws field", embed.getFields().stream().anyMatch(f -> f.getName().equals("Throws:")));
    }

    @Test
    public void getMethodWithParameter() {
        Message msg = JDoc.get("jda.getUserById(long)");
        assertEquals("JDA.getUserById(long) should be found and therefore generate embed", 1, msg.getEmbeds().size());
        MessageEmbed embed = msg.getEmbeds().get(0);
        assertTrue("Embed should have Parameters field", embed.getFields().stream().anyMatch(f -> f.getName().equals("Parameters:")));
    }

    @Test
    public void getEnumClass() {
        Message msg = JDoc.get("accounttype");
        assertEquals("AccountType should be found and therefore generate embed", 1, msg.getEmbeds().size());
        MessageEmbed embed = msg.getEmbeds().get(0);
        assertTrue("Embed should have Throws field", embed.getFields().stream().anyMatch(f -> f.getName().equals("Values:")));
    }

    @Test
    public void getInnerClass() {
        Message msg = JDoc.get("messagebuilder.formatting");
        assertEquals("MessageBuilder.Formatting should be found and therefore generate embed", 1, msg.getEmbeds().size());
    }

    @Test
    public void getValue() {
        Message msg = JDoc.get("simplelog.name");
        assertEquals("SimpleLog.name should be found and therefore generate embed", 1, msg.getEmbeds().size());
    }

    @Test
    public void getValueWithoutDescription() {
        Message msg = JDoc.get("simplelog.name");
        assertEquals("SimpleLog.name should be found and therefore generate embed", 1, msg.getEmbeds().size());
        assertEquals("Text for no avail description should be shown", "No description available!", msg.getEmbeds().get(0).getDescription());
    }

    @Test
    public void getToLongDescription() {
        Message msg = JDoc.get("restaction");
        assertEquals("RestAction should be found and therefore generate embed", 1, msg.getEmbeds().size());
        assertTrue("Text for to long description should be shown",
                msg.getEmbeds().get(0).getDescription().startsWith("Description to long. please refer to [the docs]("));
    }

    @Test
    public void getInheritedMethod() {
        Message msg = JDoc.get("user.getid");
        assertEquals("User.getId - inherited should be found and therefore generate embed", 1, msg.getEmbeds().size());
    }
}

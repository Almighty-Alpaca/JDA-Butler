import com.kantenkugel.discordbot.jdocparser.JDoc;
import net.dv8tion.jda.core.entities.Message;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JDocParserTest {

    @BeforeClass
    public static void init() {
        JDoc.init();
    }

    @Test
    public void getNonExistentClass() {
        Message msg = JDoc.get("bla");
        assertEquals("Non-Existent class should not generate Embeds", msg.getEmbeds().size(), 0);
        assertEquals("Class not Found!", msg.getRawContent());
    }
}

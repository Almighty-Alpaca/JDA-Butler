import com.kantenkugel.discordbot.moduleutils.DocParser;
import net.dv8tion.jda.core.entities.Message;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DocParserTest {

    @BeforeClass
    public static void init() {
        DocParser.init();
    }

    @Test
    public void getNonExistentClass() {
        Message msg = DocParser.get("bla");
        assertEquals("Non-Existent class should not generate Embeds", msg.getEmbeds().size(), 0);
        assertEquals("Class not Found!", msg.getRawContent());
    }
}

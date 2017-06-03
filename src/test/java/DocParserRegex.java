import com.kantenkugel.discordbot.moduleutils.DocParser;
import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DocParserRegex {
    @Test
    public void testMethodNormal() {
        Matcher matcher = DocParser.METHOD_PATTERN.matcher("void funcName(String a, int b)");
        assertTrue("Matcher can't find normal methods", matcher.find());
        assertEquals("Matcher doesn't extract function-return correctly", "void", matcher.group(1));
        assertEquals("Matcher doesn't extract function-name correctly", "funcName", matcher.group(2));
        assertEquals("Matcher doesn't extract function-params correctly", "String a, int b", matcher.group(3));
    }

    @Test
    public void testMethodDots() {
        Matcher matcher = DocParser.METHOD_PATTERN.matcher("java.lang.String funcName(java.lang.String a)");
        assertTrue("Matcher can't find methods with dots", matcher.find());
        assertEquals("Matcher doesn't extract function-args with dots correctly", "java.lang.String a", matcher.group(3));
    }

    @Test
    public void testMethodGenerics() {
        Matcher matcher = DocParser.METHOD_PATTERN.matcher("java.lang.String funcName(Collection<String> a)");
        assertTrue("Matcher can't find methods with generics", matcher.find());
        assertEquals("Matcher doesn't extract function-args with generics correctly", "Collection<String> a", matcher.group(3));
    }

    @Test
    public void testMethodArrays() {
        Matcher matcher = DocParser.METHOD_PATTERN.matcher("void funcName(byte[] a)");
        assertTrue("Matcher can't find methods with arrs", matcher.find());
        assertEquals("Matcher doesn't extract function-args with arrs correctly", "byte[] a", matcher.group(3));
    }

    @Test
    public void testMethodCombined() {
        Matcher matcher = DocParser.METHOD_PATTERN.matcher("java.lang.String getInviteUrl(java.util.Collection<Permission> permissions, byte[] arr)");
        assertTrue("Matcher can't find methods with generics + dots + arrs", matcher.find());
        assertEquals("Matcher doesn't extract function-args with generics and correctly", "java.util.Collection<Permission> permissions, byte[] arr", matcher.group(3));
    }
}

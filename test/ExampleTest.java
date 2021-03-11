import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;

public class ExampleTest {

    @Test
    public void testExpression() {
        String test = "import ioPlus;\n" +
                "class HelloWorld {\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tioPlus.printHelloWorld();\n" +
                "\t}\n" +
                "}";
		assertEquals("Program", TestUtils.parse(test).getRootNode().getKind());
	}

}

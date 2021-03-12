import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class JmmParserSyntacticalFailTest {
    private String fileName;
    private JmmParserChecker jmmParserChecker;

    @Before
    public void initialize() {
        jmmParserChecker = new JmmParserChecker();
    }

    public JmmParserSyntacticalFailTest(String fileName) {
        this.fileName = fileName;
    }

    @Parameterized.Parameters
    public static Collection jmmFiles() {
        return Arrays.asList(new Object[][] {
                { "fail/syntactical/BlowUp.jmm" },
                { "fail/syntactical/CompleteWhileTest.jmm" },
                { "fail/syntactical/LengthError.jmm" },
                { "fail/syntactical/MissingRightPar.jmm" },
                { "fail/syntactical/MultipleSequential.jmm" },
                { "fail/syntactical/NestedLoop.jmm" },
        });
    }

    @Test(expected = Exception.class)
    public void testJmmParserChecker() throws IOException {
        System.out.println("File is : " + fileName);
        jmmParserChecker.parse(fileName);
    }
}
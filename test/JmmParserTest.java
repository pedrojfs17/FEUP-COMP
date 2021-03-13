import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.Before;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class JmmParserTest {
    private String fileName;
    private String expectedResult = "Program";
    private JmmParserChecker jmmParserChecker;

    @Before
    public void initialize() {
        jmmParserChecker = new JmmParserChecker();
    }

    public JmmParserTest(String fileName) {
        this.fileName = fileName;
    }

    @Parameterized.Parameters
    public static Collection jmmFiles() {
        return Arrays.asList(new Object[][] {
                { "FindMaximum.jmm" },
                { "HelloWorld.jmm" },
                { "Lazysort.jmm" },
                { "Life.jmm" },
                { "MonteCarloPi.jmm" },
                { "QuickSort.jmm" },
                { "Simple.jmm" },
                { "TicTacToe.jmm" },
                { "WhileAndIF.jmm" },
        });
    }

    @Test
    public void testJmmParserChecker() throws IOException {
        System.out.println("File is : " + fileName);
        System.out.println(jmmParserChecker.getRoot(fileName).toJson());
        assertEquals(expectedResult, jmmParserChecker.parse(fileName));
    }

}
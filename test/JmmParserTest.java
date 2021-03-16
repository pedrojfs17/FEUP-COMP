import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.Before;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class JmmParserTest {
    private String fileName;
    private String expectedResult = "PROGRAM";
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

        JmmParserResult result = jmmParserChecker.getResult(fileName);
        JmmNode rootNode = result.getRootNode();
        System.out.println(rootNode.toJson());

        List<Report> reports = result.getReports();
        System.out.println(reports.toString());

        assertEquals(expectedResult, jmmParserChecker.parse(fileName));
    }

}
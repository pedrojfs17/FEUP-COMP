import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

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

    @Test
    public void testJmmParserChecker() throws IOException {
        System.out.println("File is : " + fileName);

        JmmParserResult result = jmmParserChecker.getResult(fileName);
        JmmNode rootNode = result.getRootNode();
        List<Report> reports = result.getReports();

        assertTrue(reports.size() > 0);
        System.out.println(reports.toString());
    }
}
import java.io.IOException;
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

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class JmmParserSemanticFailTest {
    private String fileName;
    private JmmParserChecker jmmParserChecker;

    @Before
    public void initialize() {
        jmmParserChecker = new JmmParserChecker();
    }

    public JmmParserSemanticFailTest(String fileName) {
        this.fileName = fileName;
    }

    @Parameterized.Parameters
    public static Collection jmmFiles() {
        return Arrays.asList(new Object[][] {
                { "fail/semantic/arr_index_not_int.jmm" },
                { "fail/semantic/arr_size_not_int.jmm" },
                { "fail/semantic/badArguments.jmm" },
                { "fail/semantic/binop_incomp.jmm" },
                { "fail/semantic/funcNotFound.jmm" },
                { "fail/semantic/simple_length.jmm" },
                { "fail/semantic/var_exp_incomp.jmm" },
                { "fail/semantic/var_lit_incomp.jmm" },
                { "fail/semantic/var_undef.jmm" },
                { "fail/semantic/varNotInit.jmm" },
                { "fail/semantic/extra/miss_type.jmm" },
        });
    }

    @Test(expected = Exception.class)
    public void testJmmParserChecker() throws IOException {
        System.out.println("File is : " + fileName);

        JmmParserResult result = jmmParserChecker.getResult(fileName);
        JmmNode rootNode = result.getRootNode();
        List<Report> reports = result.getReports();

        assertTrue(reports.size() > 0);
        System.out.println(reports.toString());
    }
}
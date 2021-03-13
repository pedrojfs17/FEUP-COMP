import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.Before;

import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

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
        jmmParserChecker.parse(fileName);
    }
}
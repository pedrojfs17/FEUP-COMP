import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JmmParserChecker {
    public String parse(final String fileName) throws IOException {
        File file = new File("test/fixtures/public/" + fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        return TestUtils.parse(str).getRootNode().getKind();
    }
    public JmmNode getRoot(final String fileName) throws IOException {
        File file = new File("test/fixtures/public/" + fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        return TestUtils.parse(str).getRootNode();
    }
}

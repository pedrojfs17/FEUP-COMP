import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.Properties;

import pt.up.fe.comp.TestUtils;

public class ExampleTest {

    @Test
    public void FindMaximum() throws IOException {
        File file = new File("test/fixtures/public/FindMaximum.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

	@Test
    public void HelloWorld() throws IOException {
        File file = new File("test/fixtures/public/HelloWorld.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void Lazysort() throws IOException {
        File file = new File("test/fixtures/public/Lazysort.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void Life() throws IOException {
        File file = new File("test/fixtures/public/Life.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void MonteCarloPi() throws IOException {
        File file = new File("test/fixtures/public/MonteCarloPi.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void QuickSort() throws IOException {
        File file = new File("test/fixtures/public/QuickSort.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void Simple() throws IOException {
        File file = new File("test/fixtures/public/Simple.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void TicTacToe() throws IOException {
        File file = new File("test/fixtures/public/TicTacToe.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

    @Test
    public void WhileAndIF() throws IOException {
        File file = new File("test/fixtures/public/WhileAndIF.jmm");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");

        assertEquals("Program", TestUtils.parse(str).getRootNode().getKind());
    }

}

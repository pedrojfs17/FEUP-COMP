
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
		    Jmm myParser = new Jmm(new StringReader(jmmCode));
			SimpleNode root = myParser.Program(); // returns reference to root node
            	
    		//if (root != null) root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, myParser.getReports());
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

    public static void main(String[] args) {
        System.out.println("Executing with args: " + Arrays.toString(args));
        if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }
        File outputDir = new File("out");
        var fileContents = SpecsIo.read(args[0]);
		System.out.println("Executing file: "+args[0]);

		Main compiler = new Main();
		AnalysisStage analysisStage = new AnalysisStage();
		JmmParserResult parserResult = compiler.parse(fileContents);

		JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(parserResult);
    }


}
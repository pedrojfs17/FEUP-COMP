
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

		// Syntactic analysis
		JmmParserResult parserResult = compiler.parse(fileContents);
		// Semantic analysis
		AnalysisStage analysisStage = new AnalysisStage();
		JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(parserResult);
		// Ollir
		OptimizationStage optimizationStage = new OptimizationStage();
		semanticsResult = optimizationStage.optimize(semanticsResult);
		OllirResult ollirResult = optimizationStage.toOllir(semanticsResult);
		// Jasmin
		BackendStage backendStage = new BackendStage();
		ollirResult = optimizationStage.optimize(ollirResult);
		JasminResult jasminResult = backendStage.toJasmin(ollirResult);


		Path path = Paths.get(ollirResult.getSymbolTable().getClassName() + "/");
		try {
			if (!Files.exists(path)) {
				Files.createDirectory(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter myWriter = new FileWriter(path + "/ast.json");
			myWriter.write(parserResult.toJson());
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter myWriter = new FileWriter(path + "/symbolTable.txt");
			myWriter.write(semanticsResult.getSymbolTable().toString());
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter myWriter = new FileWriter(path + "/ollir.ollir");
			myWriter.write(ollirResult.getOllirCode());
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			FileWriter myWriter = new FileWriter(path + "/jasmin.j");
			myWriter.write(jasminResult.getJasminCode());
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		jasminResult.compile(path.toFile());
    }


}
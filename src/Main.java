import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
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
import java.util.List;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
            
            Jmm.var = new HashMap<>();
            Jmm.par_count=0;
            Jmm.reports = new ArrayList<>();
        
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
        var fileContents = SpecsIo.read(args[0]);
        System.out.println("Executing file: " + args[0]);

        Main compiler = new Main();

        // Syntactic analysis
        JmmParserResult parserResult = compiler.parse(fileContents);
        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0 || parserResult.getRootNode() == null) {
            System.out.println(parserResult.getReports());
            return;
        }

        // Semantic analysis
        AnalysisStage analysisStage = new AnalysisStage();
        JmmSemanticsResult semanticsResult = analysisStage.semanticAnalysis(parserResult);
        if (TestUtils.getNumReports(semanticsResult.getReports(), ReportType.ERROR) > 0 || semanticsResult.getRootNode() == null) {
            System.out.println(semanticsResult.getReports());
            return;
        }

        // Ollir
        OptimizationStage optimizationStage = new OptimizationStage();
        boolean optimize = args.length > 1 && Arrays.asList(args).contains("-o");
        if (optimize)
            semanticsResult = optimizationStage.optimize(semanticsResult);
        OllirResult ollirResult = optimizationStage.toOllir(semanticsResult, optimize);
        if (TestUtils.getNumReports(ollirResult.getReports(), ReportType.ERROR) > 0) {
            System.out.println(ollirResult.getReports());
            return;
        }

        // Jasmin
        BackendStage backendStage = new BackendStage();
        if (args.length > 1 && Arrays.asList(args).contains("-r")) {
            List<String> argList = Arrays.asList(args);
            optimizationStage.setNumRegisters(Integer.parseInt(argList.get(argList.indexOf("-r") + 1)));
            ollirResult = optimizationStage.optimize(ollirResult);
        }
        JasminResult jasminResult = backendStage.toJasmin(ollirResult);
        if (TestUtils.getNumReports(jasminResult.getReports(), ReportType.ERROR) > 0) {
            System.out.println(jasminResult.getReports());
            return;
        }

        Path mainDir = Paths.get("ToolResults/");
        try {
            if (!Files.exists(mainDir)) {
                Files.createDirectory(mainDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path path = Paths.get("ToolResults/" + ollirResult.getSymbolTable().getClassName() + "/");
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
            myWriter.write(semanticsResult.getSymbolTable().print());
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

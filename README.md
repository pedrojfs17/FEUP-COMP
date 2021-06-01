#GROUP: 4B

|Name|Number|Grade|Contribution|
|----|------|-----|-----------|
|António Bezerra | 201806854 | 20 | 25%|
|Gonçalo Alves | 201806451 | 20 | 25%|
|Inês Silva | 201806385 | 20 | 25%|
|Pedro Seixas | 201806227 | 20 | 25%|

GLOBAL Grade of the project: 20


## SUMMARY:

Our tool can parse a `.jmm` File, detecting any syntactic and semantic errors
within it, generating an AST. It can also generate an `.ollir` file, from a given AST.
Finally, it can generate a `.jasmin` File, from a given OLLIR code.

### Compile

To compile the program, run gradle build. This will compile your classes to classes/main/java and copy the JAR file to the root directory. The JAR file will have the same name as the repository folder.
To run you have two options: Run the .class files or run the JAR.

#### Run .class

To run the .class files, do the following:
`java -cp "./build/classes/java/main/" <class_name> <arguments>`
Where <class_name> is the name of the class you want to run and <arguments> are the arguments to be passed to main().

#### Run .jar

To run the JAR, do the following command:
`java -jar <jar filename> <arguments>`
Where <jar filename> is the name of the JAR file that has been copied to the root folder, and <arguments> are the arguments to be passed to main().

### Usage

## DEALING WITH SYNTACTIC ERRORS:

Our tool captures all syntactic errors within a file and warns the user about them.

## SEMANTIC ANALYSIS:

Semantic rules that our tool abides to:
(Refer the semantic rules implemented by your tool.)

## CODE GENERATION:

### Intermediate Representations

#### Abstract Syntax Tree (AST)
Firstly, the source code is read and is transformed into an abstract syntax tree (AST).
The AST has representations for every possible entity present in the source code. 
The information in the AST is used to fill the compiler output information and in later stages to support the code 
generation and syntactical analysis.

#### OO-based Low-Level Intermediate Representation (OLLIR)

By visiting the generated AST, we generate OLLIR code, an intermediate low-level representation,
inspired by three-address code representations, to represent Java classes.
This is done in the OllirVisitor class, which we created containing functions to deal with every type
of node in the AST:
 - Generates class header
 - Generates method headers
 - Formats each variable to OLLIR format, using information from the SymbolTable - IDENTIFIER (name: a) => a.i32
 - Formats each operation to OLLIR format - OPERATION (op: + ) => +.i32
 - Constructs while loops and if/else structures
 - Formats array accesses and method calls 
 - Creates temporary variables, as needed


### JVM Instructions

The OLLIR output is used as input to the backend stage of the compiler, responsible for the selection of JVM insrtuctions,
the assignment of the local variables of methods to the local variables of the JVM and the generation 
of JVM code in the jasmin format.

The OllirResult generated in the optimization stage is used to generate a ClassUnit object which represents 
the class being compiled, including it's fields, methods, etc...

The jasmin code is iteratively written to a String as we extract the needed information from the ClassUnit, in the following order:
 - Generate imports
 - Generate class header
 - Generate fields
 - Generate methods
   - Generate method header
   - Generate instructions
     - Load variables using optimal instruction for variable type
     - Perform operation or method invocation using necessary instructions
     - Store variables using optimal instruction for variable type
     - Update stack limit
   - Generate *limit stack* and *limit locals* instructions 
   - Generate return instruction


## TASK DISTRIBUTION:

1. Develop a parser for Java--using JavaCC and taking as starting point the Java--grammar furnished (note that the original grammar may originate conflicts when implemented
with parsers of LL(1) type and in that case you need to modify the grammar in order to solve those conflicts);
2. Include error treatment and recovery mechanisms for while conditions; 
   
3. Proceed with the specification of the file jjt to generate, using JJTree, a new version of the parser including in this case the generation of the syntax tree(the generated tree should be anAST), annotating the nodes and leafs of the tree with the information (in-cluding tokens) necessary to perform the subsequent compiler steps; 

4.Implement the interfaces that will allow the generation of the JSON files representing the source code and the necessary symbol tables;

5.Implement the Semantic Analysis and generate the LLIR code, OLLIR(see document [7]), from the AST;

6.Generate from the OLLIR the JVM code accepted by jasmin corresponding to the invoca-tion of functions inJava--;

7.Generate from the OLLIR JVM code accepted by jasmin for arithmetic expressions;[checkpoint2]

8.Generate from the OLLIR JVM code accepted by jasmin for conditional instructions(ifandif-else);

9.Generate from the OLLIR JVM code accepted by jasmin for loops;

10.Generate from the OLLIR JVM code accepted by jasmin to deal with arrays.

11.Complete the compiler and test it using a set of Java--classes;[checkpoint3]

12.Proceed with the optimizations related to the code generation,related to the register al-location (“-r”option) and the optimizations related tothe “-o”option.

## PROS:

We developed a very comprehensive tool, implementing every feature specified in the project description.
Besides the organized and clean code, the most positive aspects we identified are regarding the implemented optimizations.

### Optimizations

By default, our compiler generates JVM code with lower cost instructions in the cases of iload, istore,
astore, aload, loading constants to the stack and using iinc.

After generating the **SemanticResult**, if the "-o" option is used, the following optimizations are performed: 
 - **Constant propagation and folding**
   - Replacing variables with values (except in whiles);
   - Computing operations with constant values, to eliminate unnecessary expressions;
 - **While loops**
   - Eliminates unnecessary labels and goto instructions;
   - Eg:
    - ```
        Loop0:
        if (...) goto Body0:
        goto EndLoop0;
        Body0:
        ...
        goto Loop0;
      ```
    - ```
        goto Condition0;
        Loop0:
        ...
        Condition0:
        if(...) gotoLoop0;
      ```
After generating the **OllirResult**, if the "-r" option is used, the **register allocation** optimization is performed to each method:
 - Uses *dataflow analysis* to determine the lifetime of local variables
 - Constructs the Register-Interference Graph, where each node represents a variable and each edge represents
the interference between two variables with intersecting lifetimes
 - Uses Graph Coloring to allocate registers
 - Builds a new varTable for the method, updating each variable's virtual register
 - If the specified number of registers isn't enough to store all the variables, increments this value and 
performs graph coloring again to find the minimum number of registers needed

## CONS: (Identify the most negative aspects of your tool)
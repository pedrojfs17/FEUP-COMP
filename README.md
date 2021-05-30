#GROUP: 4B

- António Bezerra, 201806854, 19, 25%
- Gonçalo Alves, 201806451, 19, 25%
- Inês Silva, 201806385, 19, 25%
- Pedro Seixas, 201806227, 19, 25%

GLOBAL Grade of the project: 19



## SUMMARY:

Our tool can parse a `.jmm` File, detecting any syntactic and semantic errors
within it, generating an AST. It can also generate an `.ollir` file, from a given AST.
Finally, it can generate a `.jasmin` File, from a given OLLIR code.

## DEALING WITH SYNTACTIC ERRORS:

Our tool captures all syntactic errors within a file and warns the user about them.

## SEMANTIC ANALYSIS:

Semantic rules that our tool abides to:
(Refer the semantic rules implemented by your tool.)

## CODE GENERATION:

(describe how the code generation of your tool works and identify the possible problems your tool has regarding code generation.)

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

## PROS: (Identify the most positive aspects of your tool)




## CONS: (Identify the most negative aspects of your tool)
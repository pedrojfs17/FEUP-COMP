#Notes

## To-Do
- [ ] Parser
  - [X] Solve Left Recursion
  - [ ] Choice conflicts: 
    - [X] VarDeclaration choice conflict (**\<IDENTIFIER>**)
    - [X] ExpressionAndMin() and \<IDENTIFIER> Statement() (**\<IDENTIFIER> [** )
    - [ ] Expressions
- [ ] Error Treatment
- [ ] Recovery Mechanism for while condition

## Grammar Table

###Original

|&nbsp;|INTEGER|IMPORT|CLASS|PUBLIC|STATIC|VOID|MAIN|STRING|EXTENDS|RETURN|INT|BOOLEAN|IF|ELSE|ELIF|WHILE|SOUT|TRUE|FALSE|THIS|NEW|LENGTH|LBRACKET|RBRACKET|LPARENTHESES|RPARENTHESES|LSQUAREBRACKET|RSQUAREBRACKET|COLON|SEMICOLON|DOT|MINUS|ADD|EQ|MULT|DIV|LESS|EXCLAMATION|AND|LETTERS|IDENTIFIER|INTEGERLITERAL
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|Type| | | | | | | | | | |-> INT LSQUAREBRACKET<br>-> INT|-> BOOLEAN| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
|Expression| | | | | | | | | | | | | | | | | | -> TRUE | -> FALSE | | -> NEW INT<br>-> NEW IDENTIFIER| | | |-> LPARENTHESES | | | | | | | | | | | | |-> EXCLAMATION| | | | -> INTEGERLITERAL |
|Statement| | | | | | | | | | | | | -> IF | | | -> WHILE| | | | | | | -> LBRACKET| | | | | | | | | | | | | | |-> EXCLAMATION| | | -> IDENTIFIER EQ<br>-> IDENTIFIER LSQUAREBRACKET | -> INTEGERLITERAL |
|VarDeclaration| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
|MethodDeclaration| | | |-> PUBLIC Type() <br> -> PUBLIC STATIC | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
|ClassDeclaration| | | -> CLASS| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
|ImportDeclaration| | -> IMPORT<br> -> nothing| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
|Program| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |

###### Missing: Expression -> Expression; Statement -> Expression; VarDeclaration -> Type; Program -> Import


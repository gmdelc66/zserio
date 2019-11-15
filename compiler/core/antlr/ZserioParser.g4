parser grammar ZserioParser;

options
{
    tokenVocab=ZserioLexer;
}

tokens { RSHIFT }

// PACKAGE (main rule)

packageDeclaration
    :   packageNameDefinition?
        importDeclaration*
        languageDirective*
        EOF
    ;

packageNameDefinition
    :   PACKAGE id (DOT id)* SEMICOLON
    ;

importDeclaration
    :   IMPORT id DOT (id DOT)* (id | MULTIPLY) SEMICOLON
    ;

languageDirective
    :   constDeclaration
    |   typeDeclaration
    ;

typeDeclaration
    :   subtypeDeclaration
    |   structureDeclaration
    |   choiceDeclaration
    |   unionDeclaration
    |   enumDeclaration
    |   sqlTableDeclaration
    |   sqlDatabaseDefinition
    |   serviceDefinition
    |   instantiateDeclaration
    ;


// CONST

constDeclaration
    :   CONST typeInstantiation id ASSIGN expression SEMICOLON
    ;


// SUBTYPE

subtypeDeclaration
    :   SUBTYPE typeReference id SEMICOLON
    ;


// STRUCTURE

structureDeclaration
    :   STRUCTURE id templateParameters? typeParameters?
        LBRACE
        structureFieldDefinition*
        functionDefinition*
        RBRACE
        SEMICOLON
    ;

structureFieldDefinition
    :   fieldAlignment?
        fieldOffset?
        OPTIONAL?
        fieldTypeId
        fieldInitializer?
        fieldOptionalClause?
        fieldConstraint?
        SEMICOLON
    ;

fieldAlignment
    :   ALIGN LPAREN DECIMAL_LITERAL RPAREN COLON
    ;

fieldOffset
    :   expression COLON
    ;

fieldTypeId
    :   IMPLICIT? typeInstantiation id fieldArrayRange?
    ;

fieldArrayRange
    :   LBRACKET expression? RBRACKET
    ;

fieldInitializer
    :   ASSIGN expression
    ;

fieldOptionalClause
    :   IF expression
    ;

fieldConstraint
    :   COLON expression
    ;


// CHOICE

choiceDeclaration
    :   CHOICE id templateParameters? typeParameters ON expression
        LBRACE
        choiceCases*
        choiceDefault?
        functionDefinition*
        RBRACE
        SEMICOLON
    ;

choiceCases
    :   choiceCase+ choiceFieldDefinition? SEMICOLON
    ;

choiceCase
    : CASE expression COLON
    ;

choiceDefault
    :   DEFAULT COLON choiceFieldDefinition? SEMICOLON
    ;

choiceFieldDefinition
    :   fieldTypeId fieldConstraint?
    ;


// UNION

unionDeclaration
    :   UNION id templateParameters? typeParameters?
        LBRACE
        unionFieldDefinition*
        functionDefinition*
        RBRACE
        SEMICOLON
    ;

unionFieldDefinition
    :   choiceFieldDefinition SEMICOLON
    ;


// ENUM

enumDeclaration
    :   ENUM typeReference id
        LBRACE
        enumItem (COMMA enumItem)* COMMA?
        RBRACE
        SEMICOLON
    ;

enumItem
    :   id (ASSIGN expression)?
    ;


// SQL TABLE

sqlTableDeclaration
    :   SQL_TABLE id templateParameters? (USING id)?
        LBRACE
        sqlTableFieldDefinition*
        sqlConstraintDefinition?
        sqlWithoutRowId?
        RBRACE
        SEMICOLON
    ;

sqlTableFieldDefinition
    :   SQL_VIRTUAL? typeInstantiation id sqlConstraint? SEMICOLON
    ;

sqlConstraintDefinition
    :   sqlConstraint SEMICOLON
    ;

sqlConstraint
    :   SQL STRING_LITERAL
    ;

sqlWithoutRowId
    :   SQL_WITHOUT_ROWID SEMICOLON
    ;


// SQL DATABASE

sqlDatabaseDefinition
    :   SQL_DATABASE id
        LBRACE
        sqlDatabaseFieldDefinition+
        RBRACE
        SEMICOLON
    ;

sqlDatabaseFieldDefinition
    :   typeInstantiation id SEMICOLON
    ;


// RPC SERVICE

serviceDefinition
    :   SERVICE id
        LBRACE
        rpcDefinition*
        RBRACE
        SEMICOLON
    ;

rpcDefinition
    :   RPC rpcTypeReference id LPAREN rpcTypeReference RPAREN SEMICOLON
    ;

rpcTypeReference
    :   STREAM? typeReference
    ;


// FUNCTION

functionDefinition
    :   FUNCTION functionType
        functionName LPAREN RPAREN // zserio functions cannot have any arguments
        functionBody
    ;

functionType
    :   typeReference
    ;

functionName
    :   id
    ;

functionBody
    :   LBRACE
        RETURN expression SEMICOLON
        RBRACE
    ;


// PARAMETERS

typeParameters
    :   LPAREN parameterDefinition (COMMA parameterDefinition)* RPAREN
    ;

parameterDefinition
    :   typeReference id
    ;


// TEMPLATES

templateParameters
    :   LT id (COMMA id)* GT
    ;

templateArguments
    :   LT templateArgument (COMMA templateArgument)* GT
    ;

templateArgument
    :   typeReference
    ;

instantiateDeclaration
    :   INSTANTIATE typeReference id SEMICOLON
    ;


// EXPRESSION

expression
    :   operator=LPAREN expression RPAREN                                           # parenthesizedExpression
    |   expression LPAREN operator=RPAREN                                           # functionCallExpression
    |   expression operator=LBRACKET expression RBRACKET                            # arrayExpression
    |   expression operator=DOT id                                                  # dotExpression
    |   operator=LENGTHOF LPAREN expression RPAREN                                  # lengthofExpression
    |   operator=VALUEOF LPAREN expression RPAREN                                   # valueofExpression
    |   operator=NUMBITS LPAREN expression RPAREN                                   # numbitsExpression
    |   operator=(PLUS | MINUS | BANG | TILDE) expression                           # unaryExpression
    |   expression operator=(MULTIPLY | DIVIDE | MODULO) expression                 # multiplicativeExpression
    |   expression operator=(PLUS | MINUS) expression                               # additiveExpression
    |   expression (operator=LSHIFT | operator=GT GT) expression                    # shiftExpression
    |   expression operator=(LT | LE | GT | GE) expression                          # relationalExpression
    |   expression operator=(EQ | NE) expression                                    # equalityExpression
    |   expression operator=AND expression                                          # bitwiseAndExpression
    |   expression operator=XOR expression                                          # bitwiseXorExpression
    |   expression operator=OR expression                                           # bitwiseOrExpression
    |   expression operator=LOGICAL_AND expression                                  # logicalAndExpression
    |   expression operator=LOGICAL_OR expression                                   # logicalOrExpression
    |   <assoc=right>expression operator=QUESTIONMARK expression COLON expression   # ternaryExpression
    |   literal                                                                     # literalExpression
    |   INDEX                                                                       # indexExpression
    |   id                                                                          # identifierExpression
    ;

literal
    :   BINARY_LITERAL
    |   OCTAL_LITERAL
    |   DECIMAL_LITERAL
    |   HEXADECIMAL_LITERAL
    |   BOOL_LITERAL
    |   STRING_LITERAL
    |   FLOAT_LITERAL
    |   DOUBLE_LITERAL
    ;

id
    :   ID
    ;


// TYPES

typeReference
    :   builtinType
    |   qualifiedName templateArguments?
    ;

typeInstantiation
    :   typeReference typeArguments?
    ;

builtinType
    :   intType
    |   varintType
    |   unsignedBitFieldType
    |   signedBitFieldType
    |   boolType
    |   stringType
    |   floatType
    ;

qualifiedName
    :   id (DOT id)*
    ;

typeArguments
    :   LPAREN typeArgument (COMMA typeArgument)* RPAREN
    ;

typeArgument
    :   EXPLICIT id
    |   expression
    ;

intType
    :   INT8
    |   INT16
    |   INT32
    |   INT64
    |   UINT8
    |   UINT16
    |   UINT32
    |   UINT64
    ;

varintType
    :   VARINT
    |   VARINT16
    |   VARINT32
    |   VARINT64
    |   VARUINT
    |   VARUINT16
    |   VARUINT32
    |   VARUINT64
    ;

unsignedBitFieldType
    :   BIT_FIELD bitFieldLength
    ;

signedBitFieldType
    :   INT_FIELD bitFieldLength
    ;

bitFieldLength
    :   COLON DECIMAL_LITERAL
    |   LT expression GT
    ;

boolType
    :   BOOL
    ;

stringType
    :   STRING
    ;

floatType
    :   FLOAT16
    |   FLOAT32
    |   FLOAT64
    ;

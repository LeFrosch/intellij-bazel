package com.google.idea.blaze.skylark.lang.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.google.idea.blaze.skylark.lang.psi.SkylarkTokenTypes;

%%

%public
%class SkylarkLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

/* numbers */
INT           = {DEC_INT} | {OCT_INT} | {HEX_INT} | "0"
DEC_INT       = [1-9][0-9]*
OCT_INT       = "0"[oO][0-7]+
HEX_INT       = "0"[oO][0-9A-Fa-f]+

FLOAT         = {DECIMALS} "." {DECIMALS}? {EXPONENT}?
              | {DECIMALS} {EXPONENT}
              | "." {DECIMALS} {EXPONENT}?
DECIMALS      = [0-9]+
EXPONENT      = [eE][+-]?{DECIMALS}

/* single-line bodies (no raw-vs-nonraw distinction here; both accept ESC) */
SDQ_BODY      = ([^\"\\\n] | {ESC})*
SSQ_BODY      = ([^\'\\\n] | {ESC})*

/* triple-quoted bodies: allow any text but avoid consuming the closing delimiter */
TDQ_BODY      = ([^\"] | \"[^\"] | \"\"[^\"] | {ESC} | \r | \n)*
TSQ_BODY      = ([^\'] | \'[^\'] | \'\'[^\'] | {ESC} | \r | \n)*

/* convenience for delimiters */
DQ3           = "\"\"\""
SQ3           = "'''"

/* escapes (don't validate semantics; just keep tokens intact) */
ESC           = \\[^]

/* string/bytes prefixes */
PFX           = "b" | "r" | "rb" | "br"

STRING        = {PFX}? \" {SDQ_BODY} \"?
              | {PFX}? \' {SSQ_BODY} \'?
              | {PFX}? {DQ3} {TDQ_BODY} {DQ3}?
              | {PFX}? {SQ3} {TSQ_BODY} {SQ3}?

IDENTIFIER    = [^\d\W]\w*
COMMENT       = "#"[^\r\n]*

LINE_CONT     = \\[\n]
NWL           = \r\n | \n | \r

%state LINE

%%

/* emmit indentation token before line starts; can be zero sized */
<YYINITIAL> {
  [\ ]+         { yybegin(LINE); return SkylarkTokenTypes.INDENT; }
  [\ ]*{NWL}    { return TokenType.WHITE_SPACE; } // filter out empty lines
  [^]           { yypushback(1); yybegin(LINE); return SkylarkTokenTypes.INDENT; }
}

/* process the line body after indentation */
<LINE> {
  [\ \t]        { return TokenType.WHITE_SPACE; }
  {NWL}         { yybegin(YYINITIAL); return SkylarkTokenTypes.NEWLINE; }
  {LINE_CONT}   { return TokenType.WHITE_SPACE; }

  {COMMENT}     { return SkylarkTokenTypes.COMMENT; }

  {STRING}      { return SkylarkTokenTypes.STRING; }
  {INT}         { return SkylarkTokenTypes.INT; }
  {FLOAT}       { return SkylarkTokenTypes.FLOAT; }

  "break"       { return SkylarkTokenTypes.BREAK; }
  "continue"    { return SkylarkTokenTypes.CONTINUE; }
  "def"         { return SkylarkTokenTypes.DEF; }
  "elif"        { return SkylarkTokenTypes.ELIF; }
  "else"        { return SkylarkTokenTypes.ELSE; }
  "for"         { return SkylarkTokenTypes.FOR; }
  "if"          { return SkylarkTokenTypes.IF; }
  "in"          { return SkylarkTokenTypes.IN; }
  "lambda"      { return SkylarkTokenTypes.LAMBDA; }
  "load"        { return SkylarkTokenTypes.LOAD; }
  "not"         { return SkylarkTokenTypes.NOT; }
  "and"         { return SkylarkTokenTypes.L_AND; }
  "or"          { return SkylarkTokenTypes.L_OR; }
  "pass"        { return SkylarkTokenTypes.PASS; }
  "return"      { return SkylarkTokenTypes.RETURN; }
  "None"        { return SkylarkTokenTypes.NONE; }
  "True"        { return SkylarkTokenTypes.TRUE; }
  "False"       { return SkylarkTokenTypes.FALSE; }

  {IDENTIFIER}  { return SkylarkTokenTypes.IDENTIFIER; }

  "+"           { return SkylarkTokenTypes.PLUS; }
  "-"           { return SkylarkTokenTypes.MINUS; }
  "*"           { return SkylarkTokenTypes.STAR; }
  "**"          { return SkylarkTokenTypes.STARSTAR; }
  "/"           { return SkylarkTokenTypes.DIV; }
  "//"          { return SkylarkTokenTypes.FLOORDIV; }
  "%"           { return SkylarkTokenTypes.MOD; }
  "~"           { return SkylarkTokenTypes.TILDE; }
  "&"           { return SkylarkTokenTypes.AND; }
  "|"           { return SkylarkTokenTypes.OR; }
  "^"           { return SkylarkTokenTypes.XOR; }
  "<<"          { return SkylarkTokenTypes.SHL; }
  ">>"          { return SkylarkTokenTypes.SHR; }
  "."           { return SkylarkTokenTypes.DOT; }
  ","           { return SkylarkTokenTypes.COMMA; }
  "="           { return SkylarkTokenTypes.ASSIGN; }
  "<"           { return SkylarkTokenTypes.LES; }
  ">"           { return SkylarkTokenTypes.GRE; }
  "<="          { return SkylarkTokenTypes.LEQ; }
  ">="          { return SkylarkTokenTypes.GEQ; }
  "=="          { return SkylarkTokenTypes.EQ; }
  "!="          { return SkylarkTokenTypes.NEQ; }
  "-="          { return SkylarkTokenTypes.MINUS_EQ; }
  "+="          { return SkylarkTokenTypes.PLUS_EQ; }
  "*="          { return SkylarkTokenTypes.MUL_EQ; }
  "/="          { return SkylarkTokenTypes.DIV_EQ; }
  "//="         { return SkylarkTokenTypes.FLOORDIV_EQ; }
  "%="          { return SkylarkTokenTypes.MOD_EQ; }
  "&="          { return SkylarkTokenTypes.AND_EQ; }
  "|="          { return SkylarkTokenTypes.OR_EQ; }
  "^="          { return SkylarkTokenTypes.XOR_EQ; }
  "<<="         { return SkylarkTokenTypes.SHL_EQ; }
  ">>="         { return SkylarkTokenTypes.SHR_EQ; }
  ";"           { return SkylarkTokenTypes.SEMICOLON; }
  ":"           { return SkylarkTokenTypes.COLON; }
  "("           { return SkylarkTokenTypes.LPAREN; }
  ")"           { return SkylarkTokenTypes.RPAREN; }
  "["           { return SkylarkTokenTypes.LBRACKET; }
  "]"           { return SkylarkTokenTypes.RBRACKET; }
  "{"           { return SkylarkTokenTypes.LBRACE; }
  "}"           { return SkylarkTokenTypes.RBRACE; }

  [^]           { return TokenType.BAD_CHARACTER; }
}

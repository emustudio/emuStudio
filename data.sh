#!/bin/bash

cd data
./lex.sh
./parser.sh

cd ..
mv data/Parser*.java src/brainc_brainduck/impl/ParserBD.java
mv data/Sym*.java src/brainc_brainduck/impl/SymBD.java
mv data/Lexer*.java src/brainc_brainduck/impl/LexerBD.java


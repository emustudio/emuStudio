#!/bin/bash

cd data
./lex.sh
./parser.sh

cd ..
mv data/Parser*.java src/as_z80/impl/Parser*.java
mv data/Sym*.java src/as_z80/impl/Sym*.java
mv data/Lexer*.java src/as_z80/impl/Lexer*.java


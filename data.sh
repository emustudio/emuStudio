#!/bin/bash

cd data
./lex.sh
./parser.sh

cd ..
mv data/RAMParser.java src/ramc_ram/impl/RAMParser.java
mv data/SymRAM.java src/ramc_ram/impl/SymRAM.java
mv data/RAMLexer.java src/ramc_ram/impl/RAMLexer.java


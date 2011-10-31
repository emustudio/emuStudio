#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="RAMParser"     # nazov triedy parsera
symbols="SymRAM"       # nazov triedy symbolov
package="ramc_ram.impl"          # aky balik ma mat parser aj symboly

echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source
#java -jar D:/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source


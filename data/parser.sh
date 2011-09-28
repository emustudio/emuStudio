#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="ParserBD"   # nazov triedy parsera
symbols="SymBD"     # nazov triedy symbolov
package="brainc_brainduck.impl"      # aky balik ma mat parser aj symboly

echo "Removing old files..."
rm -f $target.java $symbols.java *.states cup.error
echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source
#java -jar D:/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source


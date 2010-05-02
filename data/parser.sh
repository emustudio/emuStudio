#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="ParserZ80"  # nazov triedy parsera
symbols="SymZ80"    # nazov triedy symbolov
package="as_z80.impl"      # aky balik ma mat parser aj symboly

echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source


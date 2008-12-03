#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="parserZ80"  # nazov triedy parsera
symbols="symZ80"    # nazov triedy symbolov
package="impl"      # aky balik ma mat parser aj symboly

echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source


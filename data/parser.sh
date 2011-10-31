#!/bin/bash

source="parser.cup" # nazov zdrojoveho suboru
target="Parser8080"     # nazov triedy parsera
symbols="Sym8080"        # nazov triedy symbolov
package="as_8080.impl"   # aky balik ma mat parser aj symboly

echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source

#java -jar d:/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source

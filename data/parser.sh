#!/bin/bash

source="parser8080.cup" # nazov zdrojoveho suboru
target="parser8080"     # nazov triedy parsera
symbols="sym8080"        # nazov triedy symbolov
package="compiler8080"   # aky balik ma mat parser aj symboly

echo "Generating JAVA files..."
java -jar ~/bin/java-cup/java-cup-11a.jar -package $package -parser $target -symbols $symbols -interface $source


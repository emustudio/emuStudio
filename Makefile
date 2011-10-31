ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/compilers
BIN = ../bin
ZIPNAME = 'as-8080-0.31b'
BINFILES = $(BIN)/as-8080.jar ./emuLib
DISTFILES = $(BIN)/as-8080.jar

all: src nbproject asmgen
	$(ANT) clean jar
	rm -f -r $(DOC)/html $(DOC)/javadoc-$(ZIPNAME)
	doxygen
	mv $(DOC)/html $(DOC)/javadoc-$(ZIPNAME)
	rm -f -r $(EXPORT)
	mkdir -p $(EXPORT)
	$(ZIP) a $(EXPORT)/$(ZIPNAME).zip $(BINFILES)
	$(ZIP) a $(EXPORT)/javadoc-$(ZIPNAME).zip $(DOC)/javadoc-$(ZIPNAME)

asmgen: data
	cd data && ./lexer.sh
	cd data && ./parser.sh
	mv data/*.java -t src/as_8080/impl

dist: asmgen
	$(ANT) clean jar
	rm -f -r $(DIST)
	mkdir -p $(DIST)
	cp $(DISTFILES) -t $(DIST)

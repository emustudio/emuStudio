ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/compilers
BIN = ../bin
ZIPNAME = 'ramc-ram-0.14b'
BINFILES = $(BIN)/ramc-ram.jar ./emuLib ./ram-mem
DISTFILES = $(BIN)/ramc-ram.jar

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
	mv data/*.java -t src/ramc_ram/impl

dist: asmgen
	$(ANT) clean jar
	rm -f -r $(DIST)
	mkdir -p $(DIST)
	cp $(DISTFILES) -t $(DIST)

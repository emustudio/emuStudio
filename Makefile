ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
BIN = ../bin
ZIPNAME = `cat zip-name`
BINFILES = $(BIN)/brainc-brainduck.jar ./emuLib

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
	mv data/*.java -t src/brainc_brainduck/impl

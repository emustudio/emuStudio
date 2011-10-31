ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/mem
BIN = ../bin
ZIPNAME = 'standard-mem-0.30b'
BINFILES = $(BIN)/standard-mem.jar ./emuLib
DISTFILES = $(BIN)/standard-mem.jar

all: src nbproject
	$(ANT) clean jar
	rm -f -r $(DOC)/html $(DOC)/javadoc-$(ZIPNAME)
	doxygen
	mv $(DOC)/html $(DOC)/javadoc-$(ZIPNAME)
	rm -f -r $(EXPORT)
	mkdir -p $(EXPORT)
	$(ZIP) a $(EXPORT)/$(ZIPNAME).zip $(BINFILES)
	$(ZIP) a $(EXPORT)/javadoc-$(ZIPNAME).zip $(DOC)/javadoc-$(ZIPNAME)

dist:
	$(ANT) clean jar
	rm -r -f $(DIST)
	mkdir -p $(DIST)
	cp $(DISTFILES) -t $(DIST)

ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/mem
BIN = ../bin
ZIPNAME = 'brainduck-mem-0.12b'
BINFILES = $(BIN)/brainduck-mem.jar ./emuLib
DISTFILES = $(BIN)/brainduck-mem.jar

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

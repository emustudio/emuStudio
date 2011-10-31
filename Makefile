ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/cpu
BIN = ../bin
ZIPNAME = 'ram-cpu-0.12b1'
BINFILES = $(BIN)/ram-cpu.jar ./emuLib ./abstractTape ./together ./ram-mem ./ramc-ram
DISTFILES = $(BIN)/ram-cpu.jar

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
	rm -f -r $(DIST)
	mkdir -p $(DIST)
	cp $(DISTFILES) -t $(DIST)

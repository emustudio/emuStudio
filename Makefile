ANT = /usr/share/netbeans/java/ant/bin/ant
ZIP = 7z
DOC = ../doc
EXPORT = ../export
DIST = ../dist/devices
BIN = ../bin
ZIPNAME = 'terminal-brainduck-0.13b'
BINFILES = $(BIN)/terminal-brainduck.jar ./emuLib ./brainduck-cpu
DISTFILES = $(BIN)/terminal-brainduck.jar

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

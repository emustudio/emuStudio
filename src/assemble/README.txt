Welcome to emuStudio
---------------------

emuStudio is software-based computer emulation platform, intended for emulation of whole
computers (real or abstract), or their parts. The emulation is performed by plug-ins that
represent computer components. There exist also compiler plug-ins allowing direct compilation
of source code written for use in the emulator.

The platform supports versatility by categorizing computer components into three types
(CPUs, memories, and devices). The categorization is reflected by providing standard Java
interfaces that plug-ins must implement (see project [emuLib](http://github.com/vbmacher/emuLib)).

List of currently implemented plug-ins can be found on project's web site (and they can be
implemented indepedently by anyone). Most of them are available in the other repositories on
GitHub.

Main purpose of emuStudio is to allow realization of ideas in early phases of hardware design,
and support education process. The platform has been used at Technical University of Košice
(Slovakia) with very good responses of students and teachers since 2007.

Installation
------------

Please follow instructions on [project's web page](http://emustudio.sourceforge.net/downloads.html).
To run the project from the command line, type the following:

        java -jar "emuStudio.jar" 

License
-------

This project is released under GNU GPL v2 license.


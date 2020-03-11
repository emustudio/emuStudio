# emuStudio web site

This module contains the website for emuStudio. It also contains all the official documentation. Besides the user manual
it also contains wa set of tutorials for plug-in development.

All the documentation is written in Asciidoc format, but some pages use Markdown. 

## License

The website license is the same as for emuStudio (GPL v2), however the Jekyll template used by this website is licensed
with the MIT License, as follows:

        Copyright (c) 2015-2016 Nicholas Cerminara, scotch.io, LLC
        
        Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
        documentation files (the "Software"), to deal in the Software without restriction, including without limitation
        the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
        and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
        
        The above copyright notice and this permission notice shall be included in all copies or substantial portions
        of the Software.
        
        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
        TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
        THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
        CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
        DEALINGS IN THE SOFTWARE.


The template can be found [here](https://github.com/scotch-io/scotch-io.github.io).


## How to "compile" the website?

This site is using Jekyll static site generator. For more information about how to work with Jekyll,
please visit [official Jekyll site](https://jekyllrb.com/).

In order to net.emustudio.architecture.build a production version of the site, enter:

```bash
./net.emustudio.architecture.build.sh
```

Then upload the site manually to the hosting server.

The site has some dependencies which must be installed manually. The list of them is as follows:

- [Graphviz](http://www.graphviz.org/Download.php), for the `dot` program
- Ruby GEMs: `asciidoctor-diagram`, `jekyll-asciidoc` 

For testing the site locally, run the command:

```bash
jekyll serve --baseurl ''
```

Then, navigate the browser to http://localhost:4000/.

## Documentation organization

The documentation is organized in several subdirectories:

- `_docdevel/emulator_tutorial` - tutorials for developing plugins for emuStudio. People reading these tutorials should
                  be Java developers.
- `_docuser/` - user's manual. All official computers and plug-ins should be documented from the user
                perspective. Here should be also placed all programming examples used in emulated machines.

## Who and How to contribute

Anyone can contribute. If you want to extend the documentation or fix something in the site, please fork the
repository, make the changes and raise a pull request.
 
It's suggested to follow a predefined structure of the content for the particular document type.
User manual is written in different style and contains different "type" of sections than developer's tutorial.

### Extending user manual

User manual should document only official plug-ins and main-module (which are in this git repository), including some
general behavior of emuStudio as a whole. This means, things not related to emuStudio directly should not be documented.
For example, general emulation history is not part of emuStudio. These things can be mentioned in text, but with link
to the location with more detailed information.

#### Main module

Regarding main module, the following sections are important, in order:

- Main description - what is emuStudio, explaining the purpose. Next, for who it is for. Next, what can be done with
                     emuStudio - list of features, and general description of how it works.
- User interface - description of various windows used in emuStudio. Starting with windows which appears first, like
                   the open computer dialog. Then, logically explain each "panel" - source code editor, and emulator,
                   and the descriptions of related subwindows as their subsections.
- Command line - description of command line commands
- Automatic emulation - explain the purpose. And show example of using.
- Logger configuration - how to turn on/off or customize logger. Also, how to access log file.

#### Plug-ins 

Description of a plug-in usually should not be standalone, but put in a document collection describing the
whole computer. Description of each computer should be put in a separate directory, e.g. `_docuser/mits_altair_8800/`,
or `_docuser/brainduck`, etc. The undersores (`_`) will be interpreted as spaces in the website. 

The description should focus mainly on the interactive part of the emulation; describe how it works in separate
and clearly marked sections ("advanced topic"; or "for developers").

The description should start with some introduction:

- How the computer is related to the computer history?
- Is it abstract or real?
- The purpose of the computer
- Comparison of features which it has as the emulator for emuStudio with the features of real computer

Then, every plug-in should be described, starting from compiler - in the form of the "programming language" tutorial.

It is important - keep the information useful. Do not try hard to put any information if you think it is too small.
Some plug-ins are quite clear and don't seem to interact much with user, which is OK. For example usually it's the
CPU plug-in.

Programming examples should follow, if the plug-in allows programming. For example, both MITS 88-DISK and MITS 88-SIO
are programmable devices.

Then a very important section should be devoted to automatic emulation. More specifically:

- How the plug-in will behave if emuStudio will run in automatic emulation mode?
- Where can user find output files if the output is redirected to a file?
- What is the behavior if the automatic emulation is run more times in a row? Will the files be overridden or appended? 
- Can be output file names changed?

If the plug-in defines some API (in emuStudio it's called a context), the following section should talk
about how to use it in custom plug-ins, and how the interface looks like.

The last section should talk about debugging of the plug-in. For example:

- List of known bugs
- How to report a bug
- How to do some analysis when something does not work


### Extending tutorials for developing plug-ins

Documentation of plug-ins development is in the form of tutorials. They are specific for particular plug-in type, but
they start with general information. The tutorials are oriented towads building single computer - 
https://en.wikipedia.org/wiki/Manchester_Small-Scale_Experimental_Machine[SSEM (Small-Scale Experimental Machine)].
It is important to keep the focus.
 
Also, keep the order of the sections and particular tutorials as when a developer would really proceed. It is true that
every developer might do things in different order and this is for a debate. For this reason, the current order is given
as follows:

- Compiler
- Memory
- CPU
- Device(s) 

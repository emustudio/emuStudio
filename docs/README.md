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

In order to build a production version of the site, enter:

```bash
./build.sh
```

Then upload the site manually to the hosting server.

The site has some dependencies which must be installed manually. The list of them is as follows:

- [Graphviz](http://www.graphviz.org/Download.php), for the `dot` program
- Ruby GEMs: `asciidoctor-diagram`, `jekyll-asciidoc` 

For testing the site locally, run the command:

```bash
jekyll serve
```

Then, navigate the browser to http://localhost:4000/.

## Documentation organization

The documentation is organized in several subdirectories:

- `_documentation/developer/[version]/` - documentations for developing plugins for emuStudio. People reading these should be Java developers.
- `_documentation/user/[version]/` - user's manual. All official computers and plug-ins should be documented from the user perspective. Here should be also placed all programming examples used in emulated machines.

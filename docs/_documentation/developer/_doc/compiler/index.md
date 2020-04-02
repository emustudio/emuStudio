---
layout: default
title: Writing a compiler
nav_order: 3
permalink: /compiler/
---

# Writing a compiler

A compiler plugin must either implement [Compiler][compiler]{:target="_blank"} interface, or extend more bloat-free [AbstractCompiler][abstractCompiler]{:target="_blank"} class. Common practice is to utilize [JFLex][jflex]{:target="_blank"} and [Java Cup][jcup]{:target="_blank"} parser generators, which has direct runtime support in emuStudio.

Sample implementation of a compiler might look as follows (just some methods are implemented):

```java
public class CompilerImpl extends AbstractCompiler {
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("asm", "Assembler source file"),
    );

    private final LexerImpl lexer;
    private final ParserImpl parser;
    private MemoryContext<Short> memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
        super(pluginID, applicationApi, pluginSettings);
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        try {
            ContextPool pool = applicationApi.getContextPool();
            memory = pool.getMemoryContext(pluginID, MemoryContext.class);
            if (memory.getDataType() != Short.class) {
                throw new InvalidContextException(
                    "Unexpected memory cell type. Expected Short but was: " + memory.getDataType()
                );
            }
        } catch (ContextNotFoundException | InvalidContextException e) {
            System.err.println("Memory context is not available", e);
        }
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerImpl(in);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            IntelHEX hex = compileToHex(inputFileName);

            hex.generate(outputFileName);
            programLocation = hex.getProgramLocation();
            notifyInfo("Compilation was successful.\n Output file: " + outputFileName);

            if (memory != null) {
                hex.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into memory.");
            } else {
                notifyWarning("Memory is not available.");
            }
            return true;
        } catch (Exception e) {
            notifyError("Compilation error: " + e.getMessage());
            return false;
        } finally {
            notifyCompileFinish();
        }
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private IntelHEX compileToHex(String inputFileName) throws Exception {
       ...
    }
}
```

The compiler does not register any context, but when initialized it obtains optional memory context. If the memory
is available, it means that after compilation the program will be loaded in there, otherwise not.

Lexer and parser are not shown here, but they are created using mentioned [JFlex][jflex]{:target="_blank"} and [Java cup][jcup]{:target="_blank"} tools.

The compiler utilizes a helper class [IntelHEX][intelhex]{:target="_blank"} from emuLib, which generates Intel HEX files.

For more information, see the code of some existing compilers. 


[compiler]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/compiler/Compiler.html
[abstractCompiler]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/compiler/AbstractCompiler.html
[jflex]: https://www.jflex.de/
[jcup]: http://www2.cs.tum.edu/projects/cup/
[intelhex]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/runtime/helpers/IntelHEX.html

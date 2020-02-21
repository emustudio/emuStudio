set CMD_LINE_ARGS=%*

java -cp "compilers\ramc-ram.jar:mem\ram-mem.jar:lib\*" net.sf.emustudio.ram.compiler.impl.Main %CMD_LINE_ARGS%

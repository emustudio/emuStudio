set CMD_LINE_ARGS=%*

java -cp "compilers\raspc-rasp.jar:mem\rasp-mem.jar:lib\*" net.sf.emustudio.rasp.compiler.Main %CMD_LINE_ARGS%

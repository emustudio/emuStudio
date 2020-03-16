set CMD_LINE_ARGS=%*

java -cp "compiler\raspc-rasp.jar:memory\rasp-mem.jar:lib\*" net.emustudio.plugins.compiler.raspc.Main %CMD_LINE_ARGS%

set CMD_LINE_ARGS=%*

java -cp "compiler\raspc-rasp.jar:memory\rasp-mem.jar:lib\*" net.emustudio.plugins.compiler.raspc.Runner %CMD_LINE_ARGS%

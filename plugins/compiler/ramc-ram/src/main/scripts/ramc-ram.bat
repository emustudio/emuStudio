set CMD_LINE_ARGS=%*

java -cp "compiler\ramc-ram.jar:memory\ram-mem.jar:lib\*" net.emustudio.plugins.compiler.ramc.Main %CMD_LINE_ARGS%

set CMD_LINE_ARGS=%*

java -cp "compilers\as-z80.jar:lib\*" net.sf.emustudio.zilogZ80.assembler.impl.Main %CMD_LINE_ARGS%

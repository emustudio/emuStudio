/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.brainduck.terminal;

import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * VT100 state machine
 * <p>
 * https://vt100.net/emu/dec_ansi_parser
 */
public class Vt100StateMachine {
    interface Vt100State {
        Vt100State onData(int data);
    }

    public interface Vt100Dispatcher {

        /**
         * The C0 or C1 control function should be executed, which may have any one of a variety of effects, including
         * changing the cursor position, suspending or resuming communications or changing the shift states in effect.
         * There are no parameters to this action.
         * <p>
         * https://vt100.net/docs/vt220-rm/chapter4.html
         *
         * @param data
         */
        void execute(int data);

        /**
         * This action only occurs in ground state. The current code should be mapped to a glyph according to the
         * character set mappings and shift states in effect, and that glyph should be displayed. 20 (SP) and 7F (DEL)
         * have special behaviour in later VT series, as described in ground.
         *
         * @param data
         */
        void print(int data);

        /**
         * The final character of an escape sequence has arrived, so determined the control function to be executed from
         * the intermediate character(s) and final character, and execute it. The intermediate characters are available
         * because collect stored them as they arrived.
         *
         * @param data
         * @param collected
         */
        void escDispatch(int data, List<Integer> collected);

        /**
         * A final character has arrived, so determine the control function to be executed from private marker, intermediate
         * character(s) and final character, and execute it, passing in the parameter list. The private marker and
         * intermediate characters are available because collect stored them as they arrived.
         * <p>
         * Digital mostly used private markers to extend the parameters of existing X3.64-defined control functions, while
         * keeping a similar meaning. A few examples are shown in the table below.
         * <p>
         * No Private Marker	With Private Marker
         * SM, Set ANSI Modes	SM, Set Digital Private Modes
         * ED, Erase in Display	DECSED, Selective Erase in Display
         * CPR, Cursor Position Report	DECXCPR, Extended Cursor Position Report
         * <p>
         * In the cases above, csi_dispatch needn't know about the private marker at all, as long as it is passed along to
         * the control function when it is executed. However, the VT500 has a single case where the use of a private marker
         * selects an entirely different control function (DECSTBM, Set Top and Bottom Margins and
         * DECPCTERM, Enter/Exit PCTerm or Scancode Mode), so this action needs to use the private marker in its choice.
         * xterm takes the same approach for efficiency, even though it doesn't support DECPCTERM.
         * <p>
         * The selected control function will have access to the list of parameters, which it will use some or all of. If
         * more parameters are supplied than the control function requires, only the earliest parameters will be used and
         * the rest will be ignored. If too few parameters are supplied, default values will be used. If the control
         * function has no default values, defaulted parameters will be ignored; this may result in the control function
         * having no effect. For example, if the SM (Set Mode) control function is invoked with the sequence CSI 2;0;5 h,
         * the second parameter will be ignored because SM has no default value.
         *
         * @param data
         * @param collected
         * @param params
         */
        void csiDispatch(int data, List<Integer> collected, List<Integer> params);

        /**
         * This action is invoked when a final character arrives in the first part of a device control string. It
         * determines the control function from the private marker, intermediate character(s) and final character, and
         * executes it, passing in the parameter list. It also selects a handler function for the rest of the characters
         * in the control string. This handler function will be called by the put action for every character in the
         * control string as it arrives.
         * <p>
         * This way of handling device control strings has been selected because it allows the simple plugging-in of
         * extra parsers as functionality is added. Support for a fairly simple control string like
         * DECDLD (Downline Load) could be added into the main parser if soft characters were required, but the main
         * parser is no place for complicated protocols like ReGIS.
         *
         * @param data
         * @param collected
         * @param params
         * @return
         */
        Consumer<Integer> hook(int data, List<Integer> collected, List<Integer> params);

        /**
         * When a device control string is terminated by ST, CAN, SUB or ESC, this action calls the previously selected
         * handler function with an “end of data” parameter. This allows the handler to finish neatly.
         *
         * @param data
         */
        void unhook(int data);

        /**
         * When the control function OSC (Operating System Command) is recognised, this action initializes an external
         * parser (the “OSC Handler”) to handle the characters from the control string. OSC control strings are not
         * structured in the same way as device control strings, so there is no choice of parsers.
         *
         * @param data
         * @return
         */
        Consumer<Integer> oscStart(int data);

        /**
         * This action is called when the OSC string is terminated by ST, CAN, SUB or ESC, to allow the OSC handler to
         * finish neatly.
         *
         * @param data
         */
        void oscEnd(int data);
    }


    private final Vt100Dispatcher dispatcher;
    private Vt100State state;
    private final List<Integer> collected = new ArrayList<>();
    private final List<Integer> params = new ArrayList<>(); // max 16 originally

    public Vt100StateMachine(Vt100Dispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.state = GROUND;
    }

    public void cancel() {
        clear();
        state = GROUND;
    }

    public void accept(int data) {
        state = onData(data);
    }

    private Vt100State onData(int data) {
        Vt100State newState;
        switch (data) {
            case 0x9B:
                newState = CSI_ENTRY;
                break;
            case 0x9D:
                newState = OSC_STRING;
                break;
            case 0x90:
                newState = DCS_ENTRY;
                break;
            case 0x1B:
                newState = ESCAPE;
                break;
            case 0x98:
            case 0x9E:
            case 0x9F:
                newState = SOS_PM_APM_STRING;
                break;
            case 0x18:
            case 0x1A:
                dispatcher.execute(data);
                newState = GROUND;
                break;
            case 0x99:
            case 0x9A:
                dispatcher.print(data);
            case 0x9C:
                newState = GROUND;
                break;
            default:
                if (data >= 0x80 && data <= 0x8F) {
                    dispatcher.print(data);
                    newState = GROUND;
                } else if (data >= 0x91 && data <= 0x97) {
                    dispatcher.print(data);
                    newState = GROUND;
                } else {
                    newState = state.onData(data);
                }
        }
        return newState;
    }

    // This action causes the current private flag, intermediate characters, final character and parameters to be
    // forgotten. This occurs on entry to the escape, csi entry and dcs entry states, so that erroneous sequences
    // like CSI 3 ; 1 CSI 2 J are handled correctly.
    private void clear() {
        collected.clear();
        params.clear();
    }

    private List<Integer> parseParams() {
        List<Integer> numerals = new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        for (int param : params) {
            if ((param == 0x3B) && !numerals.isEmpty()) {
                char[] number = new char[numerals.size()];
                int i = 0;
                for (int n : numerals) {
                    number[i++] = (char)n;
                }
                result.add(Integer.parseInt(String.valueOf(number)));
                numerals.clear();
            } else {
                numerals.add(param);
            }
        }
        if (!numerals.isEmpty()) {
            char[] number = new char[numerals.size()];
            int i = 0;
            for (int n : numerals) {
                number[i++] = (char)n;
            }
            result.add(Integer.parseInt(String.valueOf(number)));
        }

        return result;
    }

    private final Vt100State GROUND = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0x20 && data <= 0x7F) {
                dispatcher.print((char) data);
            } else if (data >= 0 && data <= 0x17 || data == 0x19 || data >= 0x1C && data <= 0x1F) {
                dispatcher.execute(data);
            }
            return this;
        }
    };
    private final Vt100State ESCAPE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            clear();
            if (data >= 0 && data <= 0x17 || data == 0x19 || data >= 0x1C && data <= 0x1F) {
                dispatcher.execute(data);
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
                return ESCAPE_INTERMEDIATE;
            } else if (data >= 0x30 && data <= 0x4F || data >= 0x51 && data <= 0x57 || data >= 0x60 && data <= 0x7E ||
                    data == 0x59 || data == 0x5A || data == 0x5C) {
                dispatcher.escDispatch(data, List.copyOf(collected));
                return GROUND;
            }
            switch (data) {
                case 0x58:
                case 0x5E:
                case 0x5F:
                    return SOS_PM_APM_STRING;
                case 0x50:
                    return DCS_ENTRY;
                case 0x5D:
                    return OSC_STRING;
                case 0x5B:
                    return CSI_ENTRY;
            }
            return this;
        }
    };

    private final Vt100State ESCAPE_INTERMEDIATE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19) {
                dispatcher.execute(data);
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
            } else if (data >= 0x30 && data <= 0x7E) {
                dispatcher.escDispatch(data, List.copyOf(collected));
                return GROUND;
            }
            return this;
        }
    };

    private final Vt100State SOS_PM_APM_STRING = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data == 0x9C) {
                return GROUND;
            }
            return this;
        }
    };

    private final Vt100State CSI_PARAM = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19) {
                dispatcher.execute(data);
            } else if ((data >= 0x30 && data <= 0x39) || data == 0x3B) {
                params.add(data);
            } else if (data >= 0x40 && data <= 0x7E) {
                dispatcher.csiDispatch(data, List.copyOf(collected), parseParams());
                return GROUND;
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
                return CSI_INTERMEDIATE;
            } else if (data >= 0x3C && data <= 0x3F || data == 0x3A) {
                return CSI_IGNORE;
            }
            return this;
        }
    };

    private final Vt100State CSI_IGNORE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19) {
                dispatcher.execute(data);
            } else if (data >= 0x40 && data <= 0x7E) {
                return GROUND;
            }
            return this;
        }
    };

    private final Vt100State CSI_ENTRY = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            clear();
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19) {
                dispatcher.execute(data);
            } else if (data >= 0x40 && data <= 0x7E) {
                dispatcher.csiDispatch(data, List.copyOf(collected), parseParams());
                return GROUND;
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
                return CSI_INTERMEDIATE;
            } else if (data == 0x3A) {
                return CSI_IGNORE;
            } else if (data >= 0x30 && data <= 0x39 || data == 0x3B) {
                params.add(data);
                return CSI_PARAM;
            } else if (data >= 0x3C && data <= 0x3F) {
                collected.add(data);
                return CSI_PARAM;
            }
            return this;
        }
    };

    private final Vt100State CSI_INTERMEDIATE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19) {
                dispatcher.execute(data);
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
            } else if (data >= 0x30 && data <= 0x3F) {
                return CSI_IGNORE;
            } else if (data >= 0x40 && data <= 0x7E) {
                dispatcher.csiDispatch(data, List.copyOf(collected), parseParams());
                return GROUND;
            }
            return this;
        }
    };

    private final Vt100State DCS_ENTRY = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            clear();
            if (data >= 0x40 && data <= 0x7E) {
                return DCS_PASSTHROUGH;
            } else if (data >= 0x30 && data <= 0x39 || data == 0x3B) {
                params.add(data);
                return DCS_PARAM;
            } else if (data >= 0x3C && data <= 0x3F) {
                collected.add(data);
                return DCS_PARAM;
            } else if (data == 0x3A) {
                return DCS_IGNORE;
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
                return DCS_INTERMEDIATE;
            }
            return this;
        }
    };

    private final Vt100State DCS_INTERMEDIATE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
            } else if (data >= 0x30 && data <= 0x3F) {
                return DCS_IGNORE;
            } else if (data >= 0x40 && data <= 0x7E) {
                return DCS_PASSTHROUGH;
            }
            return this;
        }
    };

    private final Vt100State DCS_IGNORE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data == 0x9C) {
                return GROUND;
            }
            return this;
        }
    };

    private final Vt100State DCS_PARAM = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0x30 && data <= 0x39 || data == 0x3B) {
                params.add(data);
            } else if (data >= 0x3C && data <= 0x3F || data == 0x3A) {
                return DCS_IGNORE;
            } else if (data >= 0x20 && data <= 0x2F) {
                collected.add(data);
                return DCS_INTERMEDIATE;
            } else if (data >= 0x40 && data <= 0x7E) {
                return DCS_PASSTHROUGH;
            }
            return this;
        }
    };

    private final Vt100State DCS_PASSTHROUGH = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            Consumer<Integer> hook = dispatcher.hook(data, List.copyOf(collected), parseParams());
            try {
                if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19 || data >= 0x20 && data <= 0x7E) {
                    hook.accept(data);
                } else if (data == 0x9C) {
                    return GROUND;
                }
                return this;
            } finally {
                dispatcher.unhook(data);
            }
        }
    };

    private final Vt100State OSC_STRING = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            Consumer<Integer> handler = dispatcher.oscStart(data);
            try {
                if (data >= 0x20 && data <= 0x7F) {
                    handler.accept(data);
                } else if (data == 0x9C) {
                    return GROUND;
                }
                return this;
            } finally {
                dispatcher.oscEnd(data);
            }
        }
    };
}

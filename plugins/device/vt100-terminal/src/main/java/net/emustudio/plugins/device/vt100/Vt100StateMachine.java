/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * VT100 state machine
 * <p>
 * <a href="https://vt100.net/emu/dec_ansi_parser">dec_ansi_parser</a>
 * <a href="https://graphcomp.com/info/specs/ansi_col.html">ansi_col</a>
 */
public class Vt100StateMachine {
    /**
     * Represents a single state in the VT100 state machine.
     * Each state handles incoming data bytes and returns the next state to transition to.
     */
    interface Vt100State {
        /**
         * Processes an incoming data byte while in this state.
         *
         * @param data the incoming byte value
         * @return the next state to transition to (may be {@code this} if no transition occurs)
         */
        Vt100State onData(int data);
    }

    public interface Vt100Dispatcher {

        /**
         * The C0 or C1 control function should be executed, which may have any one of a variety of effects, including
         * changing the cursor position, suspending or resuming communications or changing the shift states in effect.
         * There are no parameters to this action.
         * <p>
         * <a href="https://vt100.net/docs/vt220-rm/chapter4.html">chapter4</a>
         *
         * @param data the C0 or C1 control character to execute
         */
        void execute(int data);

        /**
         * This action only occurs in ground state. The current code should be mapped to a glyph according to the
         * character set mappings and shift states in effect, and that glyph should be displayed. 20 (SP) and 7F (DEL)
         * have special behaviour in later VT series, as described in ground.
         *
         * @param data the printable character code to display
         */
        void print(int data);

        /**
         * The final character of an escape sequence has arrived, so determined the control function to be executed from
         * the intermediate character(s) and final character, and execute it. The intermediate characters are available
         * because collect stored them as they arrived.
         *
         * @param data      the final character of the escape sequence
         * @param collected the intermediate characters collected during the escape sequence
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
         * @param data      the final character of the CSI sequence
         * @param collected the intermediate characters and private markers collected during the sequence
         * @param params    the parsed numeric parameters of the CSI sequence
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
         * @param data      the final character that triggered the hook
         * @param collected the intermediate characters and private markers collected during the DCS introduction
         * @param params    the parsed numeric parameters of the DCS sequence
         * @return a consumer that will receive subsequent data bytes in the device control string, or {@code null}
         */
        Consumer<Integer> hook(int data, List<Integer> collected, List<Integer> params);

        /**
         * When a device control string is terminated by ST, CAN, SUB or ESC, this action calls the previously selected
         * handler function with an "end of data" parameter. This allows the handler to finish neatly.
         *
         * @param data the terminating character that ended the device control string
         */
        void unhook(int data);

        /**
         * When the control function OSC (Operating System Command) is recognised, this action initializes an external
         * parser (the "OSC Handler") to handle the characters from the control string. OSC control strings are not
         * structured in the same way as device control strings, so there is no choice of parsers.
         *
         * @param data the character that triggered the OSC start
         * @return a consumer that will receive subsequent data bytes in the OSC string, or {@code null}
         */
        Consumer<Integer> oscStart(int data);

        /**
         * This action is called when the OSC string is terminated by ST, CAN, SUB or ESC, to allow the OSC handler to
         * finish neatly.
         *
         * @param data the terminating character that ended the OSC string
         */
        void oscEnd(int data);
    }


    private final Vt100Dispatcher dispatcher;
    private Vt100State state;
    private final List<Integer> collected = new ArrayList<>();
    private final List<Integer> params = new ArrayList<>(); // max 16 originally

    // Stored handlers for DCS and OSC lifecycle
    private Consumer<Integer> dcsHandler;
    private Consumer<Integer> oscHandler;

    /**
     * Creates a new VT100 state machine with the given dispatcher.
     * The initial state is {@code GROUND}.
     *
     * @param dispatcher the dispatcher that handles VT100 actions (must not be {@code null})
     * @throws NullPointerException if dispatcher is {@code null}
     */
    public Vt100StateMachine(Vt100Dispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.state = GROUND;
    }

    /**
     * Cancels the current state machine operation.
     * Exits the current state, clears all collected data and parameters, and resets to the {@code GROUND} state.
     */
    public void cancel() {
        exitState(state, 0);
        clear();
        state = GROUND;
    }

    /**
     * Feeds a single data byte into the state machine.
     * The byte is processed according to the current state and "anywhere" transitions.
     * State entry/exit actions (such as hook/unhook for DCS, oscStart/oscEnd for OSC) are triggered
     * automatically on state transitions.
     *
     * @param data the incoming byte value to process
     */
    public void accept(int data) {
        Vt100State prevState = state;
        state = onData(data);

        if (prevState != state) {
            exitState(prevState, data);
            enterState(state, data);
        }
    }

    /**
     * Performs exit actions when leaving a state.
     * For {@code DCS_PASSTHROUGH}, calls {@link Vt100Dispatcher#unhook(int)} and clears the DCS handler.
     * For {@code OSC_STRING}, calls {@link Vt100Dispatcher#oscEnd(int)} and clears the OSC handler.
     *
     * @param exitingState the state being exited
     * @param data         the data byte that triggered the transition
     */
    private void exitState(Vt100State exitingState, int data) {
        if (exitingState == DCS_PASSTHROUGH) {
            dispatcher.unhook(data);
            dcsHandler = null;
        } else if (exitingState == OSC_STRING) {
            dispatcher.oscEnd(data);
            oscHandler = null;
        }
    }

    /**
     * Performs entry actions when entering a state.
     * For {@code DCS_PASSTHROUGH}, calls {@link Vt100Dispatcher#hook(int, List, List)} and stores the returned handler.
     * For {@code OSC_STRING}, calls {@link Vt100Dispatcher#oscStart(int)} and stores the returned handler.
     *
     * @param enteringState the state being entered
     * @param data          the data byte that triggered the transition
     */
    private void enterState(Vt100State enteringState, int data) {
        if (enteringState == DCS_PASSTHROUGH) {
            dcsHandler = dispatcher.hook(data, List.copyOf(collected), parseParams());
        } else if (enteringState == OSC_STRING) {
            oscHandler = dispatcher.oscStart(data);
        }
    }

    /**
     * Processes a data byte through the "anywhere" transitions first, then delegates to the current state
     * if no "anywhere" transition matches. Handles C1 control codes (0x80–0x9F), CAN (0x18), SUB (0x1A),
     * ESC (0x1B), and ST (0x9C) as global transitions.
     *
     * @param data the incoming byte value
     * @return the next state after processing the data byte
     */
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
            case 0x99:
            case 0x9A:
                dispatcher.execute(data);
                newState = GROUND;
                break;
            case 0x9C:
                newState = GROUND;
                break;
            default:
                if (data >= 0x80 && data <= 0x8F) {
                    dispatcher.execute(data);
                    newState = GROUND;
                } else if (data >= 0x91 && data <= 0x97) {
                    dispatcher.execute(data);
                    newState = GROUND;
                } else {
                    newState = state.onData(data);
                }
        }
        return newState;
    }

    /**
     * Clears the collected intermediate characters and parameter bytes.
     * This action causes the current private flag, intermediate characters, final character and parameters to be
     * forgotten. This occurs on entry to the escape, csi entry and dcs entry states, so that erroneous sequences
     * like {@code CSI 3 ; 1 CSI 2 J} are handled correctly.
     */
    private void clear() {
        collected.clear();
        params.clear();
    }

    /**
     * Parses the raw parameter bytes (digits and semicolons) into a list of integer parameter values.
     * Semicolons ({@code 0x3B}) act as delimiters between parameters. Missing parameters (consecutive semicolons)
     * default to {@code 0}.
     *
     * @return an ordered list of parsed integer parameters
     */
    private List<Integer> parseParams() {
        List<Integer> numerals = new ArrayList<>();
        List<Integer> result = new ArrayList<>();
        for (int param : params) {
            if (param == 0x3B) {
                if (!numerals.isEmpty()) {
                    result.add(parseNumerals(numerals));
                    numerals.clear();
                } else {
                    result.add(0); // default value for missing param
                }
            } else {
                numerals.add(param);
            }
        }
        if (!numerals.isEmpty()) {
            result.add(parseNumerals(numerals));
        }
        return result;
    }

    /**
     * Converts a list of digit character codes into a single integer value.
     *
     * @param numerals a list of digit character codes (e.g., {@code 0x33} for '3')
     * @return the parsed integer value
     * @throws NumberFormatException if the numerals do not form a valid integer
     */
    private int parseNumerals(List<Integer> numerals) {
        char[] number = new char[numerals.size()];
        int i = 0;
        for (int n : numerals) {
            number[i++] = (char) n;
        }
        return Integer.parseInt(String.valueOf(number));
    }

    /**
     * The ground state. Printable characters (0x20–0x7E) are dispatched via {@link Vt100Dispatcher#print(int)},
     * and C0 control codes are dispatched via {@link Vt100Dispatcher#execute(int)}.
     * Character 0x7F (DEL) is ignored.
     */
    private final Vt100State GROUND = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0x20 && data <= 0x7E) {
                dispatcher.print((char) data);
            } else if (data >= 0 && data <= 0x17 || data == 0x19 || data >= 0x1C && data <= 0x1F) {
                dispatcher.execute(data);
            }
            // 0x7F is ignored
            return this;
        }
    };
    /**
     * The escape state, entered when ESC (0x1B) is received.
     * Clears collected data on entry. Routes to escape intermediate, CSI entry, DCS entry, OSC string,
     * or SOS/PM/APC string states depending on the next character received. Final characters (0x30–0x7E)
     * trigger {@link Vt100Dispatcher#escDispatch(int, List)}.
     */
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

    /**
     * The escape intermediate state, entered when an intermediate character (0x20–0x2F) follows ESC.
     * Collects additional intermediate characters. Final characters (0x30–0x7E) trigger
     * {@link Vt100Dispatcher#escDispatch(int, List)} and transition to {@code GROUND}.
     */
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

    /**
     * The SOS/PM/APC string state. Entered from SOS (0x98), PM (0x9E), or APC (0x9F) control characters.
     * All data is ignored; exits are handled by "anywhere" transitions (0x9C, 0x18, 0x1A, 0x1B).
     */
    private final Vt100State SOS_PM_APM_STRING = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            // All exits (0x9C, 0x18, 0x1A, 0x1B) handled by top-level anywhere transitions
            return this;
        }
    };

    /**
     * The CSI parameter state. Collects parameter digits (0x30–0x39) and semicolons (0x3B).
     * Final characters (0x40–0x7E) trigger {@link Vt100Dispatcher#csiDispatch(int, List, List)}.
     * Intermediate characters (0x20–0x2F) cause a transition to {@code CSI_INTERMEDIATE}.
     * Invalid parameter characters cause a transition to {@code CSI_IGNORE}.
     */
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

    /**
     * The CSI ignore state. Entered when an invalid character is encountered during CSI parameter or entry parsing.
     * All characters except C0 controls and final characters (0x40–0x7E) are discarded.
     * Final characters transition to {@code GROUND} without dispatching.
     */
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

    /**
     * The CSI entry state, entered when CSI (0x9B) or ESC [ is received.
     * Clears collected data on entry. Routes to CSI parameter, CSI intermediate, or CSI ignore states
     * depending on the next character. Final characters (0x40–0x7E) immediately trigger
     * {@link Vt100Dispatcher#csiDispatch(int, List, List)}.
     */
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

    /**
     * The CSI intermediate state. Collects intermediate characters (0x20–0x2F).
     * Parameter characters (0x30–0x3F) cause a transition to {@code CSI_IGNORE}.
     * Final characters (0x40–0x7E) trigger {@link Vt100Dispatcher#csiDispatch(int, List, List)}.
     */
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

    /**
     * The DCS entry state, entered when DCS (0x90) or ESC P is received.
     * Clears collected data on entry. Routes to DCS parameter, DCS intermediate, DCS passthrough,
     * or DCS ignore states depending on the next character.
     */
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

    /**
     * The DCS intermediate state. Collects intermediate characters (0x20–0x2F).
     * Parameter characters (0x30–0x3F) cause a transition to {@code DCS_IGNORE}.
     * Final characters (0x40–0x7E) cause a transition to {@code DCS_PASSTHROUGH}.
     */
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

    /**
     * The DCS ignore state. Entered when an invalid character is encountered during DCS parsing.
     * All data is discarded; exits are handled by "anywhere" transitions.
     */
    private final Vt100State DCS_IGNORE = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            // All exits handled by top-level anywhere transitions (0x9C → GROUND)
            return this;
        }
    };

    /**
     * The DCS parameter state. Collects parameter digits (0x30–0x39) and semicolons (0x3B).
     * Invalid parameter characters cause a transition to {@code DCS_IGNORE}.
     * Intermediate characters (0x20–0x2F) cause a transition to {@code DCS_INTERMEDIATE}.
     * Final characters (0x40–0x7E) cause a transition to {@code DCS_PASSTHROUGH}.
     */
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

    /**
     * The DCS passthrough state. Data bytes are forwarded to the DCS handler established by
     * {@link Vt100Dispatcher#hook(int, List, List)} on entry to this state.
     * The hook is called once on entry (via {@link #enterState(Vt100State, int)}), and
     * {@link Vt100Dispatcher#unhook(int)} is called on exit (via {@link #exitState(Vt100State, int)}).
     * Character 0x7F is ignored.
     */
    private final Vt100State DCS_PASSTHROUGH = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0 && data <= 0x17 || data >= 0x1C && data <= 0x1F || data == 0x19 || data >= 0x20 && data <= 0x7E) {
                if (dcsHandler != null) {
                    dcsHandler.accept(data);
                }
            }
            // 0x7F: ignore
            // All exits (0x9C, 0x18, 0x1A, 0x1B, etc.) handled by top-level anywhere transitions
            // unhook() called via exitState() in accept()
            return this;
        }
    };

    /**
     * The OSC string state, entered when OSC (0x9D) or ESC ] is received.
     * Printable characters (0x20–0x7F) are forwarded to the OSC handler established by
     * {@link Vt100Dispatcher#oscStart(int)} on entry to this state.
     * {@link Vt100Dispatcher#oscEnd(int)} is called on exit (via {@link #exitState(Vt100State, int)}).
     * Exits are handled by "anywhere" transitions.
     */
    private final Vt100State OSC_STRING = new Vt100State() {
        @Override
        public Vt100State onData(int data) {
            if (data >= 0x20 && data <= 0x7F) {
                if (oscHandler != null) {
                    oscHandler.accept(data);
                }
            }
            // All exits handled by top-level anywhere transitions
            // oscEnd() called via exitState() in accept()
            return this;
        }
    };
}

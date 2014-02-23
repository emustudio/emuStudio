/*
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.gui.debugTable;

import emulib.plugins.cpu.AbstractDebugColumn;
import emulib.plugins.cpu.CPU;

public class BreakpointColumn extends AbstractDebugColumn {
    private final CPU cpu;

    public BreakpointColumn(CPU cpu) {
        super("bp", java.lang.Boolean.class, true);
        this.cpu = cpu;
    }

    @Override
    public void setDebugValue(int location, Object value) {
        try {
            boolean shouldSet = Boolean.valueOf(value.toString());
            if (shouldSet) {
                cpu.setBreakpoint(location);
            } else {
                cpu.unsetBreakpoint(location);
            }
        } catch(IndexOutOfBoundsException e) {
        }
    }

    @Override
    public Object getDebugValue(int location) {
        try {
            return cpu.isBreakpointSet(location);
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
    }

}

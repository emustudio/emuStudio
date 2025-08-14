/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package dependencies.hidden;

public class BdependsOnC {
    private final C c = new C();


    public void hi() {
        c.hi();
    }
}

/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.internal;

public class Reflection {

    /**
     * Checks if a class implements given interface.
     *
     * @param theClass     class that will be tested
     * @param theInterface interface that the class should implement
     * @return true if the class implements given interface, false otherwise
     */
    public static boolean doesImplement(Class<?> theClass, Class<?> theInterface) {
        do {
            Class<?>[] interfaces = theClass.getInterfaces();
            for (Class<?> tmpInterface : interfaces) {
                if (tmpInterface.isInterface() && tmpInterface.equals(theInterface)) {
                    return true;
                } else {
                    if (doesImplement(tmpInterface, theInterface)) {
                        return true;
                    }
                }
            }
            theClass = theClass.getSuperclass();
        } while ((theClass != null) && !theClass.equals(Object.class));

        return false;
    }
}

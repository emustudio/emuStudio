/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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

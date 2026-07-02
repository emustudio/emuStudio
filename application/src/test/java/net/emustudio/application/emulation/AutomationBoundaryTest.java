/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;

public class AutomationBoundaryTest {

    @Test
    public void automationApiDoesNotExposeGuiPackageTypes() {
        assertFalse(usesGuiType(Automation.class.getDeclaredFields()));
        assertFalse(usesGuiType(Automation.class.getDeclaredConstructors()));
        assertFalse(usesGuiType(Automation.class.getDeclaredMethods()));
    }

    private static boolean usesGuiType(Field[] fields) {
        return Arrays.stream(fields)
                .map(Field::getType)
                .anyMatch(AutomationBoundaryTest::isGuiType);
    }

    private static boolean usesGuiType(Constructor<?>[] constructors) {
        return Arrays.stream(constructors)
                .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(AutomationBoundaryTest::isGuiType);
    }

    private static boolean usesGuiType(Method[] methods) {
        return Arrays.stream(methods)
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .anyMatch(AutomationBoundaryTest::isGuiType);
    }

    private static boolean isGuiType(Class<?> type) {
        Package typePackage = type.getPackage();
        return typePackage != null && typePackage.getName().startsWith("net.emustudio.application.gui");
    }
}

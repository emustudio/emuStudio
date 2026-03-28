/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.internal;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReflectionTest {

    @Test
    public void doesImplementTraversesInheritedInterfacesAndSuperclasses() {
        assertTrue(Reflection.doesImplement(Derived.class, Root.class));
        assertTrue(Reflection.doesImplement(ImplementsChild.class, Root.class));
        assertTrue(Reflection.doesImplement(ImplementsChild.class, Child.class));
        assertFalse(Reflection.doesImplement(NoInterfaces.class, Root.class));
    }

    private interface Root {
    }

    private interface Child extends Root {
    }

    private static class Base implements Root {
    }

    private static class Derived extends Base {
    }

    private static class ImplementsChild implements Child {
    }

    private static class NoInterfaces {
    }
}

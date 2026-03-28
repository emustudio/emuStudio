/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.internal;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class HashingTest {

    @Test
    public void sha1ProducesUppercaseDigest() throws NoSuchAlgorithmException {
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", Hashing.SHA1("abc"));
    }
}

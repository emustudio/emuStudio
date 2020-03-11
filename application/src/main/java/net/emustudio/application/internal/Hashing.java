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

import net.emustudio.emulib.runtime.helpers.RadixUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashing {

    /**
     * Compute SHA-1 hash string.
     *
     * Letters in the hash string will be in upper-case.
     *
     * @param text Data to make hash from
     * @return SHA-1 hash Hexadecimal string, null if there was some error
     * @throws java.security.NoSuchAlgorithmException self-descriptive
     */
    public static String SHA1(String text) throws NoSuchAlgorithmException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash;
        md.update(text.getBytes(StandardCharsets.ISO_8859_1), 0, text.length());
        sha1hash = md.digest();
        return RadixUtils.convertToRadix(sha1hash, 16, false);
    }
}

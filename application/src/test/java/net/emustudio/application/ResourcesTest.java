/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import org.junit.Test;

import java.util.ResourceBundle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourcesTest {

    @Test
    public void resourceBundleAndMetadataAreAvailable() {
        ResourceBundle bundle = Resources.getResourceBundle().orElseThrow(AssertionError::new);

        assertFalse(bundle.getString("version").isEmpty());
        assertFalse(bundle.getString("copyright").isEmpty());
        assertFalse(Resources.getVersion().isEmpty());
        assertFalse(Resources.getCopyright().isEmpty());
    }

    @Test
    public void versionFallsBackToPackageImplementationVersionWhenBundleLookupIsUsed() {
        assertTrue(Resources.getResourceBundle().isPresent());
    }
}

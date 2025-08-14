/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

/**
 * This class represents an exception that can be raised during PluginLoader.loadPlugin method if a main plugin class
 * does not meet requirements for plugin classes.
 */
public class InvalidPluginException extends Exception {

    public InvalidPluginException(String cause) {
        super(cause);
    }

    public InvalidPluginException(String cause, Throwable e) {
        super(cause, e);
    }

}

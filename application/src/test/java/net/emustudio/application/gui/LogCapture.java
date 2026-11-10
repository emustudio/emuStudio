/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Captures messages from one logger without changing its level or other appenders. */
public final class LogCapture extends AppenderBase<ILoggingEvent> implements AutoCloseable {
    private final Logger logger;
    private final List<String> messages = new CopyOnWriteArrayList<>();

    public LogCapture(Class<?> type) {
        logger = (Logger) LoggerFactory.getLogger(type);
        setContext(logger.getLoggerContext());
        start();
        logger.addAppender(this);
    }

    @Override
    protected void append(ILoggingEvent event) {
        messages.add(event.getFormattedMessage());
    }

    public boolean contains(String message) {
        return messages.contains(message);
    }

    @Override
    public void close() {
        logger.detachAppender(this);
        stop();
    }
}

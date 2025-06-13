package com.tanmaysharma.sqliteclone.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;

/**
 * Utility class for configuring logging.
 */
public final class LoggerUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private LoggerUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Configures logging for the application.
     */
    public static void configureLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Create console appender
        ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent> consoleAppender =
                new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("console");

        // Create encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // Configure root logger
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
        rootLogger.setLevel(Level.INFO);

        // Configure application logger
        Logger appLogger = context.getLogger("com.tanmaysharma.sqliteclone");
        appLogger.setLevel(Level.DEBUG);
    }
}
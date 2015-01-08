package com.swrve.ratelimitedlogger;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

/**
 * An implementation of Logger suitable for the rate-limited log unit tests
 */
class MockLogger implements Logger {
    private static final Logger logger = LoggerFactory.getLogger(MockLogger.class);
    public int infoMessageCount;
    public int debugMessageCount;
    public int warnMessageCount;
    public int errorMessageCount;
    int traceMessageCount;
    Optional<String> infoLastMessage;

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        logger.info("[debug] "+msg);
        debugMessageCount++;
    }

    @Override
    public void debug(String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(String msg, Object... arguments) {
        debug(MessageFormatter.arrayFormat(msg, arguments).getMessage());
    }

    @Override
    public void debug(String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String msg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        logger.info("[error] "+msg);
        errorMessageCount++;
    }

    @Override
    public void error(String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(String msg, Object... arguments) {
        error(MessageFormatter.arrayFormat(msg, arguments).getMessage());
    }

    @Override
    public void error(String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String msg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        logger.info("[info] "+msg);
        infoLastMessage = Optional.of(msg);
        infoMessageCount++;
    }

    @Override
    public void info(String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(String msg, Object... arguments) {
        info(MessageFormatter.arrayFormat(msg, arguments).getMessage());
    }

    @Override
    public void info(String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String msg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        logger.info("[trace] "+msg);
        traceMessageCount++;
    }

    @Override
    public void trace(String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(String msg, Object... arguments) {
        trace(MessageFormatter.arrayFormat(msg, arguments).getMessage());
    }

    @Override
    public void trace(String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        logger.info("[warn] "+msg);
        warnMessageCount++;
    }

    @Override
    public void warn(String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(String msg, Object... arguments) {
        warn(MessageFormatter.arrayFormat(msg, arguments).getMessage());
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String msg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        throw new IllegalStateException("not supported");
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        throw new IllegalStateException("not supported");
    }
}

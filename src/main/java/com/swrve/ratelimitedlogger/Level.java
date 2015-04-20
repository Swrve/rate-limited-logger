package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
* Our supported logging levels. These match SLF4J.
*/
public enum Level {
    TRACE("trace") {
        @Override
        void log(Logger logger, String msg, Object... arguments) {
            logger.trace(msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Throwable t) {
            logger.trace(msg, t);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Object... arguments) {
            logger.trace(marker, msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Throwable t) {
            logger.trace(marker, msg, t);
        }
    },
    DEBUG("debug") {
        @Override
        void log(Logger logger, String msg, Object... arguments) {
            logger.debug(msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Throwable t) {
            logger.debug(msg, t);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Object... arguments) {
            logger.debug(marker, msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Throwable t) {
            logger.debug(marker, msg, t);
        }
    },
    INFO("info") {
        @Override
        void log(Logger logger, String msg, Object... arguments) {
            logger.info(msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Throwable t) {
            logger.info(msg, t);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Object... arguments) {
            logger.info(marker, msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Throwable t) {
            logger.info(marker, msg, t);
        }
    },
    WARN("warn") {
        @Override
        void log(Logger logger, String msg, Object... arguments) {
            logger.warn(msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Throwable t) {
            logger.warn(msg, t);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Object... arguments) {
            logger.warn(marker, msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Throwable t) {
            logger.warn(marker, msg, t);
        }
    },
    ERROR("error") {
        @Override
        void log(Logger logger, String msg, Object... arguments) {
            logger.error(msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Throwable t) {
            logger.error(msg, t);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Object... arguments) {
            logger.error(marker, msg, arguments);
        }
        @Override
        void log(Logger logger, String msg, Marker marker, Throwable t) {
            logger.error(marker, msg, t);
        }
    };

    private final String levelName;

    Level(String levelName) {
        this.levelName = levelName;
    }

    public String getLevelName() {
        return levelName;
    }

    abstract void log(Logger logger, String msg, Object... arguments);
    abstract void log(Logger logger, String msg, Throwable t);
    abstract void log(Logger logger, String msg, Marker marker, Object... arguments);
    abstract void log(Logger logger, String msg, Marker marker, Throwable t);
}

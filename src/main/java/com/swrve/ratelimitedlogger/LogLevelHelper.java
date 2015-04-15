package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;

class LogLevelHelper {

    private LogLevelHelper() {
    }

    static void log(Logger logger, Level level, String msg, Object... arguments) {
        if (Level.TRACE == level) {
            logger.trace(msg, arguments);
        } else if (Level.DEBUG == level) {
            logger.debug(msg, arguments);
        } else if (Level.INFO == level) {
            logger.info(msg, arguments);
        } else if (Level.WARN == level) {
            logger.warn(msg, arguments);
        } else if (Level.ERROR == level) {
            logger.error(msg, arguments);
        } else {
            throw new IllegalArgumentException("unsupported level " + level);
        }
    }
}
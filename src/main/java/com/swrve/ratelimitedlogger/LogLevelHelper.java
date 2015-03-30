package com.swrve.ratelimitedlogger;

import org.slf4j.Logger;

class LogLevelHelper {

    public static enum Level {

        TRACE("trace"), DEBUG("debug"), INFO("info"), WARN("warn"), ERROR("error");
        private final String levelName;

        Level(String levelName) {
            this.levelName = levelName;
        }

        public String getLevelName() {
            return levelName;
        }
    }

    private LogLevelHelper() {
    }

    public static void log(Logger logger, Level level, String msg, Object... arguments) {
        if (logger != null && level != null) {
            switch (level) {
                case TRACE:
                    logger.trace(msg, arguments);
                    break;
                case DEBUG:
                    logger.debug(msg, arguments);
                    break;
                case INFO:
                    logger.info(msg, arguments);
                    break;
                case WARN:
                    logger.warn(msg, arguments);
                    break;
                case ERROR:
                    logger.error(msg, arguments);
                    break;
            }
        }
    }
}
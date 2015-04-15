package com.swrve.ratelimitedlogger;

/**
* Our support logging levels, matching SLF4J.
*/
public enum Level {
    TRACE("trace"), DEBUG("debug"), INFO("info"), WARN("warn"), ERROR("error");

    private final String levelName;

    Level(String levelName) {
        this.levelName = levelName;
    }

    public String getLevelName() {
        return levelName;
    }
}

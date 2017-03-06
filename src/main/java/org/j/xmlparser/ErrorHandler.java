package org.j.xmlparser;

public interface ErrorHandler {
    default void handle(final Throwable throwable) {
        throwable.printStackTrace();
    }
}

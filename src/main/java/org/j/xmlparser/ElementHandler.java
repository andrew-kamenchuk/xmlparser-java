package org.j.xmlparser;

import org.w3c.dom.Element;

@FunctionalInterface
public interface ElementHandler {
    void handle(Element element) throws Throwable;
}

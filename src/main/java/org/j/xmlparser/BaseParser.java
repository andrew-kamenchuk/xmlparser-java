package org.j.xmlparser;

import javax.validation.constraints.NotNull;

import org.j.xmlparser.annotations.Handles;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class BaseParser {
    private final HashMap<String, List<ElementHandler>> handlers = new HashMap<>();

    private Executor executor = (Runnable command) -> {
        command.run();
    };

    private Executor prevExecutor = executor;

    public void setExecutor(@NotNull Executor executor) {
        prevExecutor = this.executor;
        this.executor = executor;
    }

    public void restoreExecutor() {
        Executor current = executor;
        executor = prevExecutor;
        prevExecutor = current;
    }

    public void addHandler(String tagName, final @NotNull ElementHandler handler) {
        tagName = tagName.toLowerCase();

        if (!handlers.containsKey(tagName)) {
            handlers.put(tagName, new ArrayList<>());
        }

        handlers.get(tagName).add(handler);
    }

    public void addHandler(@NotNull Object handlerObj) {
        for (Method method: handlerObj.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Handles.class)) {
                continue;
            }

            final String tagName = method.getAnnotation(Handles.class).tag();

            addHandler(tagName, (Element element) -> {
                try {
                    method.invoke(handlerObj, element);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public void parse(final String uri) throws IOException, SAXException {
        SAXParser parser;

        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Can't initialize parser", e);
        }

        parser.parse(uri, new SAXHandler());
    }


    @FunctionalInterface
    public interface ElementHandler {
        void handle(Element element);
    }

    private class SAXHandler extends DefaultHandler {
        private Document doc;
        private final Deque<Element> elements = new LinkedList<>();
        private StringBuilder textContent;

        @Override
        public void startDocument() throws SAXException {
            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("Can't instantiate dom document", e);
            }
        }

        @Override
        public void endDocument() throws SAXException {
            doc = null;
        }

        @Override
        public void startElement(final String uri, final String local, String name, final Attributes attr)
            throws SAXException {
            name = name.toLowerCase();

            if (handlers.containsKey(name) || !elements.isEmpty()) {
                Element element = doc.createElement(name);

                if (!elements.isEmpty()) {
                    elements.peek().appendChild(element);
                }

                for (int i = 0; i < attr.getLength(); i++) {
                    element.setAttribute(attr.getQName(i), attr.getValue(i));
                }

                elements.push(element);
            }
        }

        @Override
        public void endElement(final String uri, final String local, String name)
            throws SAXException {
            if (elements.isEmpty()) {
                return;
            }

            name = name.toLowerCase();

            final Element element = elements.pop();

            assert element.getTagName().equals(name);

            if (null != textContent && 0 == element.getChildNodes().getLength()) {
                element.setTextContent(textContent.toString().trim());
            }

            textContent = null;

            if (handlers.containsKey(name)) {
                for (ElementHandler handler: handlers.get(name)) {
                    executor.execute(() -> {
                        handler.handle(element);
                    });
                }
            }
        }

        @Override
        public void characters(final char[] buffer, final int start, final int length)
            throws SAXException {
            if (!elements.isEmpty()) {
                if (null == textContent) {
                    textContent = new StringBuilder();
                }

                textContent.append(buffer, start, length);
            }
        }
    }
}

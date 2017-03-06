package org.j.xmlparser;

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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

class BaseParser {
    private final HashMap<String, List<ElementHandler>> handlers = new HashMap<>();

    private Executor executor = Runnable::run;

    private ErrorHandler errorHandler = new ErrorHandler() {};

    public Executor setExecutor(final Executor executor) {
        Objects.requireNonNull(executor);

        final Executor prev = this.executor;
        this.executor = executor;

        return prev;
    }

    public ErrorHandler setErrorHandler(final ErrorHandler errorHandler) {
        Objects.requireNonNull(errorHandler);

        final ErrorHandler prev = this.errorHandler;
        this.errorHandler = errorHandler;

        return prev;
    }

    public void addHandler(String tagName, final ElementHandler handler) {
        Objects.requireNonNull(handler);

        tagName = tagName.toLowerCase();

        if (!handlers.containsKey(tagName)) {
            handlers.put(tagName, new ArrayList<>());
        }

        handlers.get(tagName).add(handler);
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

    private class SAXHandler extends DefaultHandler {
        private Document doc;
        private final Deque<Element> elements = new LinkedList<>();
        private final StringBuilder textBuffer = new StringBuilder();

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

                textBuffer.setLength(0);
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

            if (0 == element.getChildNodes().getLength()) {
                element.setTextContent(textBuffer.toString().trim());
            }

            textBuffer.setLength(0);

            if (handlers.containsKey(name)) {
                for (ElementHandler handler : handlers.get(name)) {
                    executor.execute(() -> {
                        try {
                            handler.handle(element);
                        } catch (Throwable throwable) {
                            errorHandler.handle(throwable);
                        }
                    });
                }
            }
        }

        @Override
        public void characters(final char[] buffer, final int start, final int length)
            throws SAXException {
            if (!elements.isEmpty()) {
                textBuffer.append(buffer, start, length);
            }
        }
    }
}

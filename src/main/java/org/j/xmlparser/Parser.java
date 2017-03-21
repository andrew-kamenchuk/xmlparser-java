package org.j.xmlparser;

import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import org.j.xmlparser.annotations.Handles;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import static java.lang.invoke.MethodType.methodType;

import java.lang.reflect.Method;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Parser extends BaseParser {

    public void addHandler(final Object handlerObj)
        throws NoSuchMethodException, IllegalAccessException {

        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        for (Method method : handlerObj.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Handles.class)) {
                continue;
            }

            final String tagName = method.getAnnotation(Handles.class).value();

            final String methodName = method.getName();

            final MethodHandle mh = lookup
                .findVirtual(handlerObj.getClass(), methodName, methodType(void.class, Element.class))
                .bindTo(handlerObj);

            addHandler(tagName, mh::invoke);
        }
    }

    public void parse(final String uri, final Executor executor) throws IOException, SAXException {
        final Executor prev = setExecutor(executor);
        parse(uri);
        setExecutor(prev);
    }

    public void parseFixedThreadPool(final String uri, final int nThreads) throws IOException, SAXException {
        parse(uri, Executors.newFixedThreadPool(nThreads));
    }

    public void parseCachedThreadPool(final String uri) throws IOException, SAXException {
        parse(uri, Executors.newCachedThreadPool());
    }

    public void parseSingleThreadPool(final String uri) throws IOException, SAXException {
        parse(uri, Executors.newSingleThreadExecutor());
    }
}

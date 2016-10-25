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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Parser extends BaseParser {

    public void addHandler(final Object handlerObj)
        throws NoSuchMethodException, IllegalAccessException {

        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        for (Method method : handlerObj.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Handles.class)) {
                continue;
            }

            final String tagName = method.getAnnotation(Handles.class).tag();

            final String methodName = method.getName();

            final MethodHandle mh = lookup
                .findVirtual(handlerObj.getClass(), methodName, methodType(void.class, Element.class))
                .bindTo(handlerObj);

            addHandler(tagName, mh::invoke);
        }
    }

    private boolean parseAndRestoreExecutor(final String uri, final ExecutorService executor)
        throws IOException, SAXException {

        final Executor prev = setExecutor(executor);

        parse(uri);

        executor.shutdown();

        boolean done = false;

        try {
            done = executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        setExecutor(prev);
        return done;
    }

    public boolean parseFixedThreadPool(final String uri, final int nThreads)
        throws IOException, SAXException {
        return parseAndRestoreExecutor(uri, Executors.newFixedThreadPool(nThreads));
    }

    public boolean parseCachedThreadPool(final String uri) throws IOException, SAXException {
        return parseAndRestoreExecutor(uri, Executors.newCachedThreadPool());
    }

    public boolean parseSingleThreadPool(final String uri) throws IOException, SAXException {
        return parseAndRestoreExecutor(uri, Executors.newSingleThreadExecutor());
    }
}

package org.j.xmlparser;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Parser extends BaseParser {
    private boolean parseAndCloseExecutorService(final String uri, final ExecutorService executor)
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
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        return parseAndCloseExecutorService(uri, executor);
    }

    public boolean parseCachedThreadPool(final String uri) throws IOException, SAXException {
        final ExecutorService executor = Executors.newCachedThreadPool();
        return parseAndCloseExecutorService(uri, executor);
    }

    public boolean parseSingleThreadPool(final String uri) throws IOException, SAXException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        return parseAndCloseExecutorService(uri, executor);
    }
}

package com.bharath.media_backend.util;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
    private final InputStream delegate;
    private long remaining;

    public LimitedInputStream(InputStream delegate, long limit) {
        this.delegate = delegate;
        this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int result = delegate.read();
        if (result != -1) remaining--;
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        int toRead = (int) Math.min(len, remaining);
        int count = delegate.read(b, off, toRead);
        if (count != -1) remaining -= count;
        return count;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}

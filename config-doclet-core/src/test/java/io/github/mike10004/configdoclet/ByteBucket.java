package io.github.mike10004.configdoclet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

class ByteBucket {

    private final ByteArrayOutputStream collector;

    public ByteBucket(int initCapacity) {
        collector = new ByteArrayOutputStream(initCapacity);
    }

    public OutputStream stream() {
        return collector;
    }

//    public PrintStream printStream(Charset charset) throws UnsupportedEncodingException {
//        return new PrintStream(stream(), true, charset.name());
//    }
//
    public PrintWriter printWriter(Charset charset) {
        return new PrintWriter(new OutputStreamWriter(stream(), charset), true);
    }

    public byte[] dump() {
        return collector.toByteArray();
    }

    public String dump(Charset charset) {
        return new String(dump(), charset);
    }
}

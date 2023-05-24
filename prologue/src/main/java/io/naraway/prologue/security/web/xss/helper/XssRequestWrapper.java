package io.naraway.prologue.security.web.xss.helper;

import io.naraway.prologue.security.web.xss.converter.XssConverter;
import org.apache.commons.io.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class XssRequestWrapper extends HttpServletRequestWrapper {
    //
    private byte[] rawData;
    private final HttpServletRequest request;
    private final ResettableServletInputStream servletStream;
    private final XssConverter xssConverter;

    public XssRequestWrapper(HttpServletRequest request, XssConverter xssConverter) {
        //
        super(request);
        this.request = request;
        this.servletStream = new ResettableServletInputStream();
        this.xssConverter = xssConverter;
    }

    public void resetInputStream(byte[] data) {
        //
        servletStream.stream = new ByteArrayInputStream(data);
    }

    @Override
    @SuppressWarnings("java:S1874")
    public ServletInputStream getInputStream() throws IOException {
        //
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getReader());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }

        return servletStream;
    }

    @Override
    @SuppressWarnings("java:S1874")
    public BufferedReader getReader() throws IOException {
        //
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getReader());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }

        return new BufferedReader(new InputStreamReader(servletStream));
    }

    @Override
    public String getParameter(String name) {
        return this.xssConverter.convert(super.getParameter(name));
    }

    @Override
    @SuppressWarnings("java:S1168")
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);

        if (values == null) {
            return null;
        }

        String[] filteredValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            filteredValues[i] = this.xssConverter.convert(values[i]);
        }

        return filteredValues;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        //
        Map<String, String[]> parameterMap = super.getParameterMap();
        Map<String, String[]> filteredParameterMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null) {
                filteredParameterMap.put(entry.getKey(), null);
            } else {
                String[] filteredValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    filteredValues[i] = this.xssConverter.convert(values[i]);
                }
                filteredParameterMap.put(entry.getKey(), filteredValues);
            }
        }

        return filteredParameterMap;
    }

    private class ResettableServletInputStream extends ServletInputStream {
        //
        private InputStream stream;

        @Override
        public int read() throws IOException {
            //
            return stream.read();
        }

        @Override
        public boolean isFinished() {
            //
            return false; // skip
        }

        @Override
        public boolean isReady() {
            //
            return false; // skip
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // skip
        }
    }
}
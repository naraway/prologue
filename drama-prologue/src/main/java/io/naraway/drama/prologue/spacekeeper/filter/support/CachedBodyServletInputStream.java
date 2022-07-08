/*
 COPYRIGHT (c) NEXTREE Inc. 2014
 This software is the proprietary of NEXTREE Inc.
 @since 2014. 6. 10.
 */

package io.naraway.drama.prologue.spacekeeper.filter.support;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CachedBodyServletInputStream extends ServletInputStream {
    //
    private final InputStream cachedBodyInputStream;

    public CachedBodyServletInputStream(byte[] cachedBody) {
        //
        this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public boolean isFinished() {
        //
        try {
            return cachedBodyInputStream.available() == 0;
        } catch (IOException e) {
            // do nothing
        }

        return false;
    }

    @Override
    public boolean isReady() {
        //
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        //
        throw new UnsupportedOperationException();
    }

    @Override
    public int read() throws IOException {
        //
        return cachedBodyInputStream.read();
    }
}

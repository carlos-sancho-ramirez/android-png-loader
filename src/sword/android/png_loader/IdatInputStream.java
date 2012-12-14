package sword.android.png_loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Assert;

public class IdatInputStream extends InputStream {

    private static final byte[] PNG_IDAT_TYPE = {
        (byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54 };

    private final Iterator<Chunk> mIterator;
    private Chunk mCurrentChunk; // null only if there is no more Idat chunks to read
    private InputStream mInputStream;

    public IdatInputStream(List<Chunk> chunks) {
        mIterator = chunks.iterator();
        getNextIdatChunk();
    }

    private void getNextIdatChunk() {
        do {
            try {
                mCurrentChunk = mIterator.next();
            }catch(NoSuchElementException exception) {
                mCurrentChunk = null;
                return;
            }
        }while(!mCurrentChunk.isType(PNG_IDAT_TYPE));

        mInputStream = mCurrentChunk.getDataInputStream();
    }

    @Override
    public int read() throws IOException {

        if (mCurrentChunk == null) {
            return -1;
        }

        int value = mInputStream.read();
        if (value == -1) {
            mInputStream.close();
            mInputStream = null;

            getNextIdatChunk();
            return read();
        }else{
            Assert.assertTrue(value >= 0 && value <= 255);
            return value;
        }
    }

    @Override
    public void close() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
        }

        super.close();
    }
}

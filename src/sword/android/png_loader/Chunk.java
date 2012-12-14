package sword.android.png_loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A Chunk is composed by:
 * * A 32-bit big-endian integer which says the length OF THE DATA
 *   (it means excluding everything that is not data)
 * * A group of 4 chars (8 bits each) that identifies the Chunk type.
 * * A byte array whose length is described at the beginning of this chunk.
 *   This binary data must be interpreted in a different way depending on the
 *   type of chunk.
 * * 32-bit CRC (big-endian), this is calculated from the type signature and the
 *   data of the chunk, but not the length.
 */
public class Chunk {

    private static final int TYPE_FIELD_LENGTH = 4;

    private byte[] mType; // Always 4 bytes
    private byte[] mData;

    public int getBigEndianAt(int offset) {

        int length = 0;
        int untilOffset = offset + 4;

        for(int i=offset; i<untilOffset; i++) {

            int value = mData[i];
            if (value < 0) value += 256;

            length = (length << 8) + value;
        }

        return length;
    }

    public int getByteAt(int offset) {
        int value = mData[offset];
        if (value < 0) value += 256;

        return value;
    }

    public Chunk(InputStream inStream) throws IOException, UnexpectedEndOfFileException {

        int length = Utils.loadBigEndianInt(inStream, 4);

        mType = new byte[TYPE_FIELD_LENGTH];
        Utils.loadToBuffer(inStream, mType);

        if (length != 0) {
            mData = new byte[length];
            Utils.loadToBuffer(inStream, mData);
        }

        // This is the CRC
        // TODO: CRC should be checked.
        Utils.loadBigEndianInt(inStream, 4);
    }

    public InputStream getDataInputStream() {
        return new ByteArrayInputStream(mData);
    }

    public String getTypeString() {
        return new String(mType);
    }

    public boolean isType(final byte[] type) {
        return Arrays.equals(type, mType);
    }
}

package sword.android.png_loader;

import java.io.IOException;
import java.io.InputStream;

public final class Utils {

    // No instances of this class are allowed
    private Utils() {}

    public static void loadToBuffer(InputStream inStream, final byte[] buffer) throws IOException, UnexpectedEndOfFileException {
        int value;
        int offset = 0;

        while(offset < buffer.length) {
            value = inStream.read(buffer, offset, buffer.length - offset);
            if (value < 0) {
                throw new UnexpectedEndOfFileException();
            }

            offset += value;
        }
    }

    public static int loadBigEndianIntWithSubFilter(InputStream inStream, final byte[] refInt) throws IOException, UnexpectedEndOfFileException {
        byte[] diffData = new byte[refInt.length];
        int readBytes = 0;
        while(readBytes < diffData.length) {
            int readNow = inStream.read(diffData, readBytes, diffData.length - readBytes);
            readBytes += readNow;
            if (readNow < 0) {
                throw new UnexpectedEndOfFileException();
            }
        }

        int result = 0;
        int value;
        for(int i=0; i<diffData.length; i++) {
            refInt[i] += diffData[i];

            value = refInt[i];
            if (value < 0) {
                value += 256;
            }
            result = (result << 8) + value;
        }

        return result;
    }

    public static int loadBigEndianInt(InputStream inStream, int bytes) throws IOException, UnexpectedEndOfFileException {
        int value;
        int length = 0;

        for(int i=0; i<bytes; i++) {
            value = inStream.read();
            if (value < 0 || value > 255) {
                throw new UnexpectedEndOfFileException();
            }

            length = (length << 8) + value;
        }

        return length;
    }

    public static int loadLittleEndianInt(InputStream inStream, int bytes) throws IOException, UnexpectedEndOfFileException {
        int value;
        int length = 0;

        for(int i=0; i<bytes; i++) {
            value = inStream.read();
            if (value < 0 || value > 255) {
                throw new UnexpectedEndOfFileException();
            }

            length |= value << (8 * i);
        }

        return length;
    }

    public static int getBigEndianInt(byte[] bytes) {
        int value;
        int length = 0;

        for(int i=0; i<bytes.length; i++) {
            value = bytes[i];
            if (value < 0) {
                value += 256;
            }

            length = (length << 8) + value;
        }

        return length;
    }

    public static int unsignedSum(byte... bytes) {
        int result = 0;
        for(int i=0; i<bytes.length; i++) {
            result += (bytes[i]<0)? bytes[i] + 256 : bytes[i];
        }

        return result;
    }

    public static byte unsignedAverage(byte... bytes) {
        return (byte)(unsignedSum(bytes) / bytes.length);
    }
}

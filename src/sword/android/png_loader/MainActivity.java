package sword.android.png_loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.InflaterInputStream;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PNG_FILE_NAME = "dice.png";
    private static final byte[] PNG_HEADER = {
        (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A };

    private static final byte[] PNG_IHDR_TYPE = {
        (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52 };

    private static final byte[] PNG_IEND_TYPE = {
        (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44 };

    private static final int PNG_IHDR_WIDTH_OFFSET = 0;
    private static final int PNG_IHDR_HEIGHT_OFFSET = 4;
    private static final int PNG_IHDR_BIT_DEPTH_OFFSET = 8;
    private static final int PNG_IHDR_COLOR_TYPE_OFFSET = 9;
    private static final int PNG_IHDR_COMPRESSION_METHOD_OFFSET = 10;
    private static final int PNG_IHDR_FILTER_METHOD_OFFSET = 11;
    private static final int PNG_IHDR_INTERLACE_METHOD_OFFSET = 12;

    private static final int PNG_IHDR_COLOR_TYPE_PALETTE_FLAG = 0x01;
    private static final int PNG_IHDR_COLOR_TYPE_RGB_FLAG = 0x02; // If not, it is a single color channel for gray scale.
    private static final int PNG_IHDR_COLOR_TYPE_ALPHA_FLAG = 0x04;

    // There are different filterTypes that can be applied
    // to each scanline in order to improve the compression.
    // Check the following link for more information:
    // http://www.fileformat.info/format/png/corion.htm
    private static final int SCANLINE_FILTER_TYPE_NONE = 0;
    private static final int SCANLINE_FILTER_TYPE_SUB = 1;
    private static final int SCANLINE_FILTER_TYPE_UP = 2;
    private static final int SCANLINE_FILTER_TYPE_AVERAGE = 3;
    private static final int SCANLINE_FILTER_TYPE_PAETH = 4;

    private List<Chunk> extractChunks(InputStream inStream) throws IOException,
            UnexpectedEndOfFileException, WrongFileFormatException {

        byte[] header = new byte[PNG_HEADER.length];
        Utils.loadToBuffer(inStream, header);

        if (!Arrays.equals(header, PNG_HEADER)) {
            throw new WrongFileFormatException();
        }

        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        Chunk chunk = null;
        do {
            chunk = new Chunk(inStream);
            chunks.add(chunk);
        }while(!chunk.isType(PNG_IEND_TYPE));

        return chunks;
    }

    private Bitmap decodifyPngFile(InputStream inStream) throws
            WrongFileFormatException, UnexpectedEndOfFileException, IOException {

        List<Chunk> chunks = extractChunks(inStream);

        // First must be always the IHDR chunk
        Chunk header = chunks.get(0);
        if (!header.isType(PNG_IHDR_TYPE)) {
            throw new WrongFileFormatException("IHDR chunk should be the first one.");
        }

        int imageWidth = header.getBigEndianAt(PNG_IHDR_WIDTH_OFFSET);
        int imageHeight = header.getBigEndianAt(PNG_IHDR_HEIGHT_OFFSET);
        int bitDepth = header.getByteAt(PNG_IHDR_BIT_DEPTH_OFFSET);

        // BitDepth can be 1, 2, 4, 8 or 16 in the standard PNG. But only 8
        // and 16 are currently supported in this implementation.
        if ((bitDepth & 0x07) != 0) {
            throw new UnsupportedOperationException(
                    "Only bit depths of 8 or 16 are currently supported in " +
                    "this implementation.");
        }

        int colorType = header.getByteAt(PNG_IHDR_COLOR_TYPE_OFFSET);
        boolean hasPalette = (colorType & PNG_IHDR_COLOR_TYPE_PALETTE_FLAG) != 0;

        if (hasPalette) {
            throw new UnsupportedOperationException(
                    "Palette is not currently supported in this implementation.");
        }

        int compressionMethod = header.getByteAt(PNG_IHDR_COMPRESSION_METHOD_OFFSET);
        if (compressionMethod != 0) {
            throw new UnsupportedOperationException(
                    "Compression method number " + compressionMethod +
                    " is not currently supported in this implementation.");
        }

        int filterMethod = header.getByteAt(PNG_IHDR_FILTER_METHOD_OFFSET);
        if (filterMethod != 0) {
            throw new UnsupportedOperationException(
                    "Filter method number " + filterMethod +
                    " is not currently supported in this implementation.");
        }

        int interlaceMethod = header.getByteAt(PNG_IHDR_INTERLACE_METHOD_OFFSET);
        if (interlaceMethod != 0) {
            throw new UnsupportedOperationException(
                    "Interlace method number " + interlaceMethod +
                    " is not currently supported in this implementation.");
        }

        boolean isRGB = (colorType & PNG_IHDR_COLOR_TYPE_RGB_FLAG) != 0;
        boolean hasAlpha = (colorType & PNG_IHDR_COLOR_TYPE_ALPHA_FLAG) != 0;

        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight,
                Bitmap.Config.ARGB_8888);
        int channels = (isRGB ? 3 : 1) + (hasAlpha ? 1 : 0);
        int bytesPerChannel = bitDepth >> 3;
        int colorLength = channels * bytesPerChannel;
        int scanLineLengthWithoutFilter = colorLength * imageWidth;

        InputStream rawInStream = new InflaterInputStream(new IdatInputStream(chunks));

        byte[] previousScanLine = null;
        byte[] thisScanLine = null;
        byte[] thisColor = new byte[colorLength];

        for(int row = 0; row<imageHeight; row++ ) {

            int filterType = Utils.loadLittleEndianInt(rawInStream, 1);

            if (filterType != SCANLINE_FILTER_TYPE_NONE &&
                    filterType != SCANLINE_FILTER_TYPE_SUB &&
                    filterType != SCANLINE_FILTER_TYPE_UP &&
                    filterType != SCANLINE_FILTER_TYPE_AVERAGE &&
                    filterType != SCANLINE_FILTER_TYPE_PAETH) {
                throw new UnsupportedOperationException("Scanline number "
                        + row + " has as value filter " + filterType + ". Unsupported.");
            }

            if (filterType == SCANLINE_FILTER_TYPE_UP ||
                    filterType == SCANLINE_FILTER_TYPE_AVERAGE ||
                    filterType == SCANLINE_FILTER_TYPE_PAETH) {
                previousScanLine = thisScanLine;
                thisScanLine = null;
            }else{
                previousScanLine = null;
            }

            if (thisScanLine == null) {
                thisScanLine = new byte[scanLineLengthWithoutFilter];
            }

            Utils.loadToBuffer(rawInStream, thisScanLine);

            int colorPosition = 0;
            for(int column = 0; column<imageWidth; column++) {

                switch (filterType) {

                case SCANLINE_FILTER_TYPE_SUB:
                    if (column != 0) {
                        for(int i=0; i<colorLength; i++) {
                            thisColor[i] = (byte)(thisScanLine[colorPosition + i]
                                    + thisScanLine[colorPosition + i - colorLength]);
                            thisScanLine[colorPosition + i] = thisColor[i];
                        }
                    }
                    break;

                case SCANLINE_FILTER_TYPE_UP:
                    if (row != 0) {
                        for(int i=0; i<colorLength; i++) {
                            thisColor[i] = (byte)(thisScanLine[colorPosition + i]
                                    + previousScanLine[colorPosition + i]);
                            thisScanLine[colorPosition + i] = thisColor[i];
                        }
                    }
                    break;

                case SCANLINE_FILTER_TYPE_AVERAGE:
                    for (int i=0; i<colorLength; i++) {
                        byte average = 0;
                        if (row == 0 && column != 0) {
                            average = thisScanLine[colorPosition - colorLength + i];
                        }else if (row != 0 && column == 0) {
                            average = previousScanLine[colorPosition + i];
                        }else if (row != 0 && column != 0) {
                            average = Utils.unsignedAverage(
                                    thisScanLine[colorPosition - colorLength + i],
                                    previousScanLine[colorPosition + i]);
                        }

                        thisColor[i] = (byte)(thisScanLine[colorPosition + i] + average);
                        thisScanLine[colorPosition + i] = thisColor[i];
                    }
                    break;

                case SCANLINE_FILTER_TYPE_PAETH:
                    for (int i=0; i<colorLength; i++) {

                        byte paethValue = 0;
                        if (row == 0 && column != 0) {
                            paethValue = Utils.paethPrediction(
                                    thisScanLine[colorPosition - colorLength + i],
                                    (byte)0, (byte)0);

                        }else if (row != 0 && column == 0) {
                            paethValue = Utils.paethPrediction( (byte)0,
                                    previousScanLine[colorPosition + i], (byte)0);

                        }else if (row != 0 && column != 0) {
                            paethValue = Utils.paethPrediction(
                                    thisScanLine[colorPosition - colorLength + i],
                                    previousScanLine[colorPosition + i],
                                    previousScanLine[colorPosition + i - colorLength]);
                        }

                        thisColor[i] = (byte)(thisScanLine[colorPosition + i] + paethValue);
                        thisScanLine[colorPosition + i] = thisColor[i];
                    }
                    break;

                case SCANLINE_FILTER_TYPE_NONE:
                default:
                    for(int i=0; i<colorLength; i++) {
                        thisColor[i] = thisScanLine[colorPosition + i];
                    }
                }

                if (hasAlpha) {
                    byte alpha = thisColor[colorLength - 1];
                    for(int i=colorLength-1; i>0; i--) {
                        thisColor[i] = thisColor[i - 1];
                    }
                    thisColor[0] = alpha;
                }

                int color = Utils.getBigEndianInt(thisColor);

                if (!isRGB) {
                    color |= color << 16 | color << 8;
                }

                if (!hasAlpha) {
                    color |= 0xFF000000;
                }

                bitmap.setPixel(column, row, color);

                colorPosition += colorLength;
            }
        }

        rawInStream.close();
        return bitmap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Bitmap bitmap = decodifyPngFile(getAssets().open(PNG_FILE_NAME));

            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bitmap);
            setContentView(iv);

        }catch(Exception exception) {

            // This should not happen, but in case it happen I want to be
            // notified on the mobile screen.
            TextView tv = new TextView(this);
            tv.setText(exception.getClass().getSimpleName());
            exception.printStackTrace();

            setContentView(tv);
        }
    }
}

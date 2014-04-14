package cs276.assignments;

import javax.print.DocFlavor;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VBIndex implements BaseIndex {

    private static final int Integer_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int INVALID_VBCODE = -1;

    /**
     * Encodes integer array using gap encoding
     * @param inputDocIdsOutputGaps
     */
    private static void GapEncode(int[] inputDocIdsOutputGaps) {
        int curr = 0, prev;
        for (int i = 0; i < inputDocIdsOutputGaps.length; i++) {
            prev = curr;
            curr = inputDocIdsOutputGaps[i];
            inputDocIdsOutputGaps[i] = curr - prev;
        }
    }

    /**
     * Decodes gaps using gap decoding
     * @param inputGapsOutputDocIds
     */
    public static void GapDecode(int[] inputGapsOutputDocIds) {
        for (int i = 1; i < inputGapsOutputDocIds.length; i++) {
            inputGapsOutputDocIds[i] = inputGapsOutputDocIds[i - 1] + inputGapsOutputDocIds[i];
        }
    }


    /**
     * Encodes gaps using VB coding and output to FileChannel
     *
     * @param inputGaps
     * @param numGaps
     * @param fc
     */
    private static void VBEncode(int[] inputGaps, int numGaps, FileChannel fc) {
        int totalNumBytes = 0;
        byte encodedInt[] = new byte[Integer.SIZE/7 + 1];
        ByteBuffer[] buffer = new ByteBuffer[2];
        buffer[0] = ByteBuffer.allocate(Integer_BYTES); // stores total number of bytes
        buffer[1] = ByteBuffer.allocate(numGaps * (Integer.SIZE/7 + 1) + Integer_BYTES); // stores postings
        for (int i = 0; i < numGaps; i++) {
            int numBytes = VBEncodeInteger(inputGaps[i], encodedInt);
            totalNumBytes += numBytes;
            for (int j = 0; j < numBytes; j++) {
                buffer[1].put(encodedInt[j]);
            }
        }
        buffer[0].putInt(totalNumBytes);
        buffer[0].flip();
        buffer[1].flip();
        try {
            fc.write(buffer);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            buffer[0].clear();
            buffer[1].clear();
        }

    }

    /**
     * Encodes an integer using VB encoding
     * @param gap
     * @param outputVBCode
     * @return
     */
    private static int VBEncodeInteger(int gap, byte[] outputVBCode) {
        int numBytes = 0, numBits = 0;
        for (int i = 0; i < 32; i++) {
            if ((gap >> i & 0x01) != 0) {
                numBits = i;
            }
        }
        int mask = 0x7f;
        numBytes = numBits / 7 + 1;
        for (int i = 0; i < numBytes; i++) {
            outputVBCode[numBytes - 1 - i] = (byte)(gap & mask);
            if (i == 0) {
                outputVBCode[numBytes - 1 - i] |= 0x80;
            }
            gap >>= 7;
        }
        return numBytes;
    }

    /**
     * Use VB decoding to decode an integer from a byte array
     * @param inputVBCode
     * @param startIndex
     * @param numberEndIndex
     */
    private static void VBDecodeInteger(byte[] inputVBCode, int startIndex, int[] numberEndIndex) {
        // Fill in your code here
        int nextIndex = startIndex;
        numberEndIndex[0] = 0;
        Boolean success = false;
        for (int i = 0; i < 5; i++) {
            numberEndIndex[0] <<=7;
            numberEndIndex[0] += (inputVBCode[nextIndex] & 0x7f);
            if ((inputVBCode[nextIndex++] & 0x80) != 0) {
                success = true;
                break;
            }
        }
        if (!success) {
            numberEndIndex[0] = INVALID_VBCODE;
            numberEndIndex[1] = startIndex;
        } else {
            numberEndIndex[1] = nextIndex;
        }
    }


    @Override
    public PostingList readPosting(FileChannel fc) {
        ByteBuffer[] buffer = new ByteBuffer[3];
        buffer[0] = ByteBuffer.allocate(Integer_BYTES);
        buffer[1] = ByteBuffer.allocate(Integer_BYTES);
        try {
            fc.read(buffer[0]); // get termId
            buffer[0].rewind();
            fc.read(buffer[1]); // get numBytes
            buffer[1].rewind();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int termId = buffer[0].getInt();
        buffer[0].clear();
        int numBytes = buffer[1].getInt();
        buffer[1].clear();

        // get the whole posting list
        buffer[2] = ByteBuffer.allocate(numBytes);
        try {
            fc.read(buffer[2]);
            buffer[2].rewind();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // count the number of gaps
        int numGaps = 0;
        byte[] inputVBCode = new byte[numBytes];
        byte b;
        for (int i = 0; i < numBytes; i++) {
            b = buffer[2].get();
            inputVBCode[i] = b;
            if ((b & 0x80) != 0) {
                numGaps++;
            }
        }
        // use VBDecodeInteger to decode and prepare input for GapDecode
        int[] gaps = new int[numGaps];
        int[] numberEndIndex = new int[2];
        numberEndIndex[1] = 0;
        for (int i = 0; i < numGaps; i++) {
            VBDecodeInteger(inputVBCode, numberEndIndex[1], numberEndIndex);
            gaps[i] = numberEndIndex[0];
        }
        // use gap decode and write to disk
        GapDecode(gaps);
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < numGaps; i++) {
            l.add(gaps[i]);
        }
        if (numGaps > 0) {
            return new PostingList(termId, l);
        }
        return null;
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        // first write termId
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES);
        buffer.putInt(p.getTermId());
        buffer.flip();
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // prepare input for GapEncode
        int[] arr = new int[p.getList().size()];
        Iterator<Integer> iter = p.getList().iterator();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = iter.next();
        }
        // use gap encoding the input so that we can store less
        GapEncode(arr);
        // use VB encoding and write to disk
        VBEncode(arr, arr.length, fc);
    }
}

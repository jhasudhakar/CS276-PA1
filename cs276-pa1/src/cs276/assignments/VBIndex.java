package cs276.assignments;

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

public class VBIndex implements BaseIndex {

    private static final int Integer_BYTES = 4;

    private static void VBEncode(Iterator<Integer> inputGaps, int numGaps, FileChannel fc) {
        byte encodedInt[] = new byte[Integer.SIZE/7 + 1];
        ByteBuffer buffer = ByteBuffer.allocate(numGaps * (Integer.SIZE/7 + 1) + Integer_BYTES);
        while (inputGaps.hasNext()) {
            int numBytes = VBEncodeInteger(inputGaps.next(), encodedInt);
            for (int i = 0; i < numBytes; i++) {
                buffer.put(encodedInt[i]);
            }
        }
        buffer.put((byte)0xff);
        buffer.flip();
        try {
            fc.write(buffer);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            buffer.clear();
        }

    }

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

    @Override
    public PostingList readPosting(FileChannel fc) {
        /*
         * Your code here
         */
        return null;
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        // get iterator
        Iterator<Integer> iter = p.getList().iterator();
        // write postings
        VBEncode(iter, p.getList().size(), fc);
    }
}

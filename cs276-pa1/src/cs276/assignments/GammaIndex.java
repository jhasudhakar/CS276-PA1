package cs276.assignments;

import cs276.util.FileChannelUtil;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class GammaIndex implements BaseIndex {
    private static final int Integer_BYTES = Integer.SIZE / Byte.SIZE;

    /**
     * Encodes a number using unary code.  The unary code for the number is placed in the BitSet
     * outputUnaryCode starting at index startIndex.  The method returns the BitSet index that
     * immediately follows the end of the unary encoding.  Use startIndex = 0 to place the unary
     * encoding at the beginning of the outputUnaryCode.
     * <p>
     * Examples:
     * If number = 5, startIndex = 3, then unary code 111110 is placed in outputUnaryCode starting
     * at the 4th bit position and the return value 9.
     *
     * @param number           The number to be unary encoded
     * @param outputUnaryCode  The unary code for number is placed into this BitSet
     * @param startIndex       The unary code for number starts at this index position in outputUnaryCode
     * @return                 The next index position in outputUnaryCode immediately following the unary code for number
     */
    public static int UnaryEncodeInteger(int number, BitSet outputUnaryCode, int startIndex) {
        int nextIndex = startIndex;
        outputUnaryCode.set(nextIndex, nextIndex+number, true);
        nextIndex += number;
        outputUnaryCode.set(nextIndex++, false);
        return nextIndex;
    }

    /**
     * Decodes the unary coded number in BitSet inputUnaryCode starting at (0-based) index startIndex.
     * The decoded number is returned in numberEndIndex[0] and the index position immediately following
     * the encoded value in inputUnaryCode is returned in numberEndIndex[1].
     *
     * @param inputUnaryCode  BitSet containing the unary code
     * @param startIndex      Unary code starts at this index position
     * @param numberEndIndex  Return values: index 0 holds the decoded number; index 1 holds the index
     *                        position in inputUnaryCode immediately following the unary code.
     */
    public static void UnaryDecodeInteger(BitSet inputUnaryCode, int startIndex, int[] numberEndIndex) {
        int zeroIndex = inputUnaryCode.nextClearBit(startIndex);
        numberEndIndex[0] = zeroIndex - startIndex;
        numberEndIndex[1] = zeroIndex + 1;
    }

    /**
     * Return index of first non-zero bit.
     * If number is 0, return -1;
     */
    public static int FNZBit(int number) {
        int pos = Integer.SIZE - 1;
        while (pos >= 0 && (number & (1<<pos)) == 0)
            --pos;
        return pos;
    }

    /**
     * Gamma encodes number.  The encoded bits are placed in BitSet outputGammaCode starting at
     * (0-based) index position startIndex.  Returns the index position immediately following the
     * encoded bits.  If you try to gamma encode 0, then the return value should be startIndex (i.e.,
     * it does nothing).
     *
     * @param number            Number to be gamma encoded
     * @param outputGammaCode   Gamma encoded bits are placed in this BitSet starting at startIndex
     * @param startIndex        Encoded bits start at this index position in outputGammaCode
     * @return                  Index position in outputGammaCode immediately following the encoded bits
     */
    public static int GammaEncodeInteger(int number, BitSet outputGammaCode, int startIndex) {
        int nextIndex = startIndex;
        int pos = FNZBit(number);
        // pos is now at the most significant 1-bit
        if (pos < 0)
            return nextIndex;

        nextIndex = UnaryEncodeInteger(pos, outputGammaCode, nextIndex);
        --pos;
        // output offset
        while (pos >= 0) {
            outputGammaCode.set(nextIndex++, (number & (1<<pos)) > 0);
            --pos;
        }

        return nextIndex;
    }

    /**
     * Decodes the Gamma encoded number in BitSet inputGammaCode starting at (0-based) index startIndex.
     * The decoded number is returned in numberEndIndex[0] and the index position immediately following
     * the encoded value in inputGammaCode is returned in numberEndIndex[1].
     *
     * @param inputGammaCode  BitSet containing the gamma code
     * @param startIndex      Gamma code starts at this index position
     * @param numberEndIndex  Return values: index 0 holds the decoded number; index 1 holds the index
     *                        position in inputGammaCode immediately following the gamma code.
     */
    public static void GammaDecodeInteger(BitSet inputGammaCode, int startIndex, int[] numberEndIndex) {
        // get length of offset
        UnaryDecodeInteger(inputGammaCode, startIndex, numberEndIndex);
        int count = numberEndIndex[0];
        int nextIndex = numberEndIndex[1];

        // reconstruct number
        int number = 1;
        for (int i = 0;i < count;++i) {
            number <<= 1;
            number |= inputGammaCode.get(nextIndex++) ? 1 : 0;
        }

        numberEndIndex[0] = number;
        numberEndIndex[1] = nextIndex;
    }


    @Override
    public PostingList readPosting(FileChannel fc) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES);
        // read number of bits first
        FileChannelUtil.readFromFileChannel(fc, buffer);

        if (!buffer.hasRemaining())
            return null;

        int bits = buffer.getInt();
        int bytes = numBytes(bits);
        buffer = ByteBuffer.allocate(bytes);
        // read in all bits
        FileChannelUtil.readFromFileChannel(fc, buffer);
        BitSet gammaCodes = bytesToBitSet(buffer, bytes);

        int[] numberIndex = new int[2];
        GammaDecodeInteger(gammaCodes, 0, numberIndex);
        int termId = numberIndex[0];
        int nextIndex = numberIndex[1];

        int prevId = 0;
        List<Integer> postings = new ArrayList<Integer>();
        while (nextIndex < bits) {
            GammaDecodeInteger(gammaCodes, nextIndex, numberIndex);
            int gap = numberIndex[0];
            nextIndex = numberIndex[1];

            prevId += gap;
            postings.add(prevId);
        }

        return new PostingList(termId, postings);
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        BitSet gammaCodes = new BitSet();

        // encode term ID
        int nextIndex = GammaEncodeInteger(p.getTermId(), gammaCodes, 0);

        // encode all (gapped) doc IDs
        int prevId = 0;
        Iterator<Integer> iter = p.getList().iterator();
        while (iter.hasNext()) {
            int currId = iter.next();
            // GammaEncodeInteger will ignore 0, i.e. duplicate doc IDs
            nextIndex = GammaEncodeInteger(currId-prevId, gammaCodes, nextIndex);
            prevId = currId;
        }

        // Q: is it possible gammaCodes.length() reaches or even exceeds Integer.MAX?
        assert gammaCodes.length() > 0;

        int bytes = numBytes(gammaCodes.length());
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES + bytes);
        buffer.putInt(gammaCodes.length());
        bitSetToBytes(gammaCodes, buffer);

        FileChannelUtil.writeToFileChannel(fc, buffer);
    }

    /**
     * Return the minimal number of bytes to hold bits.
     *
     * @param bits
     * @return
     */
    private static int numBytes(int bits) {
        if (bits == 0) return 0;
        return (bits-1) / Byte.SIZE + 1;
    }

    /**
     * Convert a BitSet to bytes and output to the ByteBuffer.
     *
     * @param stream
     * @param buffer
     */
    private static void bitSetToBytes(BitSet stream, ByteBuffer buffer) {
        byte block = 0;
        for (int i = 0;i < stream.length();++i) {
            block |= (stream.get(i) ? 1 : 0) << (i%Byte.SIZE);
            if ((i+1)%Byte.SIZE == 0) {
                // block is full, flush
                buffer.put(block);
                // reset all bits
                block = 0;
            }
        }

        if (stream.length()%Byte.SIZE != 0) {
            // not byte-aligned, flush the trailing part
            buffer.put(block);
        }
    }

    /**
     * Convert bytes from the ByteBuffer to a BitSet.
     *
     * @param buffer
     * @param limit number of bytes to read
     * @return
     */
    private static BitSet bytesToBitSet(ByteBuffer buffer, int limit) {
        BitSet bitSet = new BitSet();
        int index = 0;
        for (int i = 0;i < limit;++i) {
            byte block = buffer.get();
            for (int j = 0;j < Byte.SIZE;++j) {
                boolean one = (block & (1<<j)) > 0;
                bitSet.set(index++, one);
            }
        }

        return bitSet;
    }
}

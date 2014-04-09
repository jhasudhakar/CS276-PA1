package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

public class BasicIndex implements BaseIndex {
    private static final int BUFFER_LIMIT = 1024;
    // Integer.BYTES only available in Java 8
    private static final int Integer_BYTES = 4;

    /**
     *
     * @param fc a FileChannel whose position is at the beginning of a posting list
     * @return a PostList
     */
    @Override
    public PostingList readPosting(FileChannel fc) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES* BUFFER_LIMIT);

        readFromFileChannel(fc, buffer);
        if (!buffer.hasRemaining())
            return null;

        // the writing guarantees that there are at least 2 integers to read
        // 1. the termID and 2. posting list boundary (-1)
        int termID = buffer.getInt();
        int docID;
        ArrayList<Integer> postings = new ArrayList<Integer>();

        boolean reachedEnd = false;
        while (!reachedEnd) {
            while (buffer.hasRemaining()) {
                docID = buffer.getInt();
                if (docID == -1) {
                    reachedEnd = true;

                    // rewind to correct position in FileChannel
                    // as we might read beyond posting list boundary
                    adjustFileChannelPosition(fc, -buffer.remaining());
                    break;
                }

                postings.add(docID);
            }

            // assert (reachedEnd || !buffer.hasRemaining());

            if (!reachedEnd) {
                readFromFileChannel(fc, buffer);
            }
        }

        return new PostingList(termID, postings);
    }

    /**
     * Adjust fc's position by offset bytes.
     * @param fc
     * @param offset negative value means move forward
     */
    private static void adjustFileChannelPosition(FileChannel fc, int offset) {
        try {
            fc.position(fc.position() + offset);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES * BUFFER_LIMIT);
        buffer.putInt(p.getTermId());
        Iterator<Integer> iterator = p.getList().iterator();

        int counter = 1; // already put 1 integer (termID)
        while (iterator.hasNext()) {
            if (counter == BUFFER_LIMIT) {
                // buffer is full, flush to file
                writeToFileChannel(fc, buffer);
                counter = 0;
            }

            buffer.putInt(iterator.next());
            counter++;
        }

        // assert (counter >= 1);

        // write remaining data
        // as we will also write the boundary (-1), should first check
        // if the buffer is full
        if (counter == BUFFER_LIMIT)
            writeToFileChannel(fc, buffer);

        buffer.putInt(-1); // posting list boundary
        // compact buffer so it's ready to write
        buffer.flip();
        writeToFileChannel(fc, buffer);
    }

    private static void writeToFileChannel(FileChannel fc, ByteBuffer buffer) {
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buffer.clear();
        }
    }

    private static void readFromFileChannel(FileChannel fc, ByteBuffer buffer) {
        try {
            // clear buffer
            buffer.clear();
            // read into buffer
            fc.read(buffer);
            // compact buffer
            buffer.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

public class BasicIndex implements BaseIndex {
    private static final int BUFFER_LIMIT = 1024;

    /**
     *
     * @param fc a FileChannel whose position is at the beginning of a posting list
     * @return a PostList
     */
    @Override
	public PostingList readPosting(FileChannel fc) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * BUFFER_LIMIT);

        readFromFileChannel(fc, buffer);
        if (!buffer.hasRemaining())
            return null;

        int termID = buffer.getInt();
        int docID;
        ArrayList<Integer> postings = new ArrayList<Integer>();

        boolean reachedEnd = false;
        while (!reachedEnd) {
            while (buffer.hasRemaining()) {
                docID = buffer.getInt();
                if (docID == -1) {
                    reachedEnd = true;

                    // set correct position in FileChannel
                    adjustFileChannelPosition(fc, -buffer.remaining());
                    break;
                }

                postings.add(docID);
            }

            if (!reachedEnd) {
                readFromFileChannel(fc, buffer);
            }
        }

        return new PostingList(termID, postings);
	}

    private static void adjustFileChannelPosition(FileChannel fc, int offset) {
        try {
            fc.position(fc.position() + offset);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void writePosting(FileChannel fc, PostingList p) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * BUFFER_LIMIT);
        buffer.putInt(p.getTermId());
        Iterator<Integer> iterator = p.getList().iterator();

        int counter = 1; // already put 1 integer
        while (iterator.hasNext()) {
            if (counter == BUFFER_LIMIT) {
                // buffer is full, write to file
                writeToFileChannel(fc, buffer);
                counter = 0;
            }

            buffer.putInt(iterator.next());
            counter++;
        }

        // write trailing data
        if (counter == BUFFER_LIMIT)
            writeToFileChannel(fc, buffer);

        buffer.putInt(-1); // posting list boundary
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

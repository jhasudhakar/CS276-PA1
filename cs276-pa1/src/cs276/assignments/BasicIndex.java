package cs276.assignments;

import cs276.util.FileChannelUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

public class BasicIndex implements BaseIndex {
    // Integer.BYTES only available in Java 8
    private static final int Integer_BYTES = 4;

    /**
     *
     * @param fc a FileChannel whose position is at the beginning of a posting list
     * @return a PostList
     */
    @Override
    public PostingList readPosting(FileChannel fc) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES * 2);

        // read term ID and posting size
        FileChannelUtil.readFromFileChannel(fc, buffer);
        if (!buffer.hasRemaining())
            return null;

        int termId = buffer.getInt();
        int numDocs = buffer.getInt();
        ArrayList<Integer> postings = new ArrayList<Integer>(numDocs);

        buffer = ByteBuffer.allocate(Integer_BYTES * numDocs);
        FileChannelUtil.readFromFileChannel(fc, buffer);
        for (int i = 0;i < numDocs;++i) {
            int docId = buffer.getInt();
            postings.add(docId);
        }

        return new PostingList(termId, postings);
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        // a PostingList is stored on disk as:
        //     |term ID|list length|doc ID1|doc ID2|...|doc IDn|
        // all value are int32.
        int numIntegers = 1 + 1 + p.getList().size();
        ByteBuffer buffer = ByteBuffer.allocate(Integer_BYTES * numIntegers);

        buffer.putInt(p.getTermId());
        buffer.putInt(p.getList().size());

        for (int docId : p.getList())
            buffer.putInt(docId);

        FileChannelUtil.writeToFileChannel(fc, buffer);
    }

}

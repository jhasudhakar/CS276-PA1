package cs276.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelUtil {
    public static void writeToFileChannel(FileChannel fc, ByteBuffer buffer) {
        try {
            buffer.flip();
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buffer.clear();
        }
    }

    public static void readFromFileChannel(FileChannel fc, ByteBuffer buffer) {
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
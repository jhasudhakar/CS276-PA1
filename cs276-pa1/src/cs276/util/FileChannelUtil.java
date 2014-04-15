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

    public static void writeToFileChannel(FileChannel fc, ByteBuffer[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i].flip();
        }
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            for (int i = 0; i < buffer.length; i++) {
                buffer[i].clear();
            }
        }
    }

    public static void readFromFileChannel(FileChannel fc, ByteBuffer buffer) {
        try {
            // clear buffer
            buffer.clear();
            // read into buffer
            fc.read(buffer);
            // compact buffer
            buffer.rewind();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readFromFileChannel(FileChannel fc, ByteBuffer[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i].clear();
        }
        try {
            fc.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < buffer.length; i++) {
            buffer[i].rewind();
        }
    }
}
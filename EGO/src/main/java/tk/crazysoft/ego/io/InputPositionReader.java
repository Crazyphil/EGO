package tk.crazysoft.ego.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

public class InputPositionReader extends FilterReader {
    private long position;

    public InputPositionReader(Reader in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int bite = super.read();
        if (bite > -1) {
            position++;
        }
        return bite;
    }

    @Override
    public int read(char[] buffer, int offset, int count) throws IOException {
        int read = super.read(buffer, offset, count);
        if (read > -1) {
            position += read;
        }
        return read;
    }

    public long getPosition() {
        return position;
    }
}

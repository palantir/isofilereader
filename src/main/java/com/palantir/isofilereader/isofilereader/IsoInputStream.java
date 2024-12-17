/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.isofilereader.isofilereader;

import com.palantir.isofilereader.isofilereader.iso.types.IsoFormatConstant;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

/**
 * A InputStream for ISOs, given for a single file, and can be read instead of dumping all bytes into memory.
 */
public class IsoInputStream extends InputStream {
    private static final int MAX_SKIP_BUFFER_SIZE = 2048;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final RandomAccessFile file;
    private final long startingLoc;
    private final long endLoc;

    /**
     * Take in a raw RandomAccessFile and this library's GenericInternalIsoFile to return an Inputstream of the file.
     * This allows for streaming file data to an application without loadingthe file into memory, this is useful for
     * larger files.
     *
     * @param file RandomAccessFile of the raw ISO
     * @param subFile GenericInternalIsoFile or (subtype of UdfInternalDataFile/IsoFormatInternalDataFile) to get
     * @throws IOException occurs when reading the underlying file fails
     */
    public IsoInputStream(RandomAccessFile file, GenericInternalIsoFile subFile) throws IOException {
        this.file = file;
        this.startingLoc = subFile.getLogicalSectorLocation() * IsoFormatConstant.BYTES_PER_SECTOR;
        this.endLoc = startingLoc + subFile.getSize();
        this.file.seek(subFile.getLogicalSectorLocation() * IsoFormatConstant.BYTES_PER_SECTOR);
    }

    /**
     * A more raw constructor for an IsoInputStream, give the original file, and a start with length to read.
     *
     * @param file RandomAccessFile of the raw ISO
     * @param start start location in bytes of the subfile
     * @param length length of the subfile
     * @throws IOException occurs when reading the underlying file fails
     */
    public IsoInputStream(RandomAccessFile file, long start, long length) throws IOException {
        this.file = file;
        this.startingLoc = start;
        this.endLoc = startingLoc + length;
        this.file.seek(start);
    }

    /**
     * Read one byte of data.
     * @return a byte in INT format
     * @throws IOException error reading byte
     */
    @Override
    public int read() throws IOException {
        if (file.getFilePointer() == endLoc) {
            return -1;
        }
        return file.read();
    }

    /**
     * This is mostly taken from <a href="https://github.com/openjdk/jdk/blob/jdk-18%2B37/src/java.base/share/classes/java/io/InputStream.java">JDK 18 InputStream</a>,
     * this is compatible with java 11.
     *
     * @param byteArray array to put data into
     * @param off offset to start in array
     * @param len length to read
     * @return how many bytes were read
     * @throws IOException failure to read underlying media
     */
    @Override
    public int read(byte[] byteArray, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int bytesRead;
        if ((off + len + file.getFilePointer()) >= endLoc) {
            int coveredLen = (int) (endLoc - (file.getFilePointer() - off));
            bytesRead = file.read(byteArray, off, coveredLen);
            if (bytesRead == 0) {
                return -1;
            }
        } else {
            bytesRead = file.read(byteArray, off, len);
        }
        return bytesRead;
    }

    /**
     * Read a number of bytes into the array provided.
     * @param bytes   the buffer into which the data is read.
     * @return number of bytes read
     * @throws IOException can error if underlying media issue
     */
    @Override
    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    /**
     * Skip Bytes.
     * @param byteNumber   the number of bytes to be skipped.
     * @return the actual number of bytes skipped
     * @throws IOException If there are errors with the underlying read.
     */
    @Override
    public long skip(long byteNumber) throws IOException {
        long remaining = byteNumber;
        int nr;

        if (byteNumber <= 0) {
            return 0;
        }

        int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
        byte[] skipBuffer = new byte[size];
        while (remaining > 0) {
            nr = read(skipBuffer, 0, (int) Math.min(size, remaining));
            if (nr < 0) {
                break;
            }
            remaining -= nr;
        }
        return byteNumber - remaining;
    }

    /**
     * An estimate of available bytes.
     *
     * @return int of bytes left
     * @throws IOException if there is a failure to read
     */
    @Override
    public int available() throws IOException {
        return (int) (endLoc - file.getFilePointer());
    }

    /**
     * Position in the file.
     * @return long of position in file
     * @throws IOException error getting current location pointer
     */
    public long position() throws IOException {
        return file.getFilePointer() - startingLoc;
    }

    /**
     * Get the length of the file being read.
     * @return long of length
     */
    public long getLength() {
        return endLoc - startingLoc;
    }

    /**
     * Seek into the file.
     * @param seekLoc bytes to seek in
     * @throws IOException if seek error occurs, either too far or failed ot read at that location
     */
    @SuppressWarnings("for-rollout:PreferSafeLoggableExceptions")
    public void seek(long seekLoc) throws IOException {
        if (startingLoc + seekLoc > getLength()) {
            throw new IOException("Seeking past end of file");
        }
        file.seek(startingLoc + seekLoc);
    }

    /**
     * Read all bytes from current location to end of file. Keep in mind, if any bit of the stream has been read
     * already this will not be a complete file. Do a "reset()" first to read the while file.
     *
     * @return byte array of data in the file
     * @throws IOException error in reading underlying media
     */
    @Override
    public byte[] readAllBytes() throws IOException {
        int size = (int) (endLoc - file.getFilePointer());
        return readNBytes(size);
    }

    /**
     * Get the next N bytes as specified in len of the file, this continues from where the last read was.
     *
     * @param len the maximum number of bytes to read
     * @return byte array of data in area
     * @throws IOException error in reading underlying media
     */
    @SuppressWarnings({"ReadReturnValueIgnored", "for-rollout:PreferSafeLoggableExceptions"})
    @Override
    public byte[] readNBytes(int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        byte[] byteArray = new byte[len];
        read(byteArray, 0, len);
        return byteArray;
    }

    /**
     * Read len bytes into off offset of an array, which is provided.
     * @param bytes the byte array into which the data is read
     * @param off the start offset in {@code b} at which the data is written
     * @param len the maximum number of bytes to read
     * @return number of bytes read
     * @throws IOException was there an error in underlying media
     */
    @Override
    public int readNBytes(byte[] bytes, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, bytes.length);

        int numTracker = 0;
        while (numTracker < len) {
            int count = read(bytes, off + numTracker, len - numTracker);
            if (count < 0) {
                break;
            }
            numTracker += count;
        }
        return numTracker;
    }

    /**
     * Close the underlying ISO read handle.
     * @throws IOException closing a file can have an exception if media is missing or already closed
     */
    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    @SuppressWarnings("StrictUnusedVariable")
    public synchronized void mark(int readlimit) {}

    /**
     * Reset this InputStream to the start of the file.
     *
     * @throws IOException failure moving offset in the underlying file
     */
    @Override
    public synchronized void reset() throws IOException {
        file.seek(startingLoc);
    }

    /**
     * Marks if marker setting is supported by this input stream, it currently is not.
     *
     * @return always false right now
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Transfer the data of an InputStream to an Outputstream. Note: This does not reset InputStream location.
     * @param out the output stream, non-null
     * @return length of file transferred in bytes
     * @throws IOException read IO exception can occur if there is a read error with the underlying media
     */
    @SuppressWarnings("for-rollout:PreferSafeLoggingPreconditions")
    @Override
    public long transferTo(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = this.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }
}

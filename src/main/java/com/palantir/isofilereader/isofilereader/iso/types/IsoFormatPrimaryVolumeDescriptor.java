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

package com.palantir.isofilereader.isofilereader.iso.types;

import com.palantir.isofilereader.isofilereader.Util;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 * Class to load and get data from the ISO Primary Volume Descriptor.
 */
@SuppressWarnings("StrictUnusedVariable")
public class IsoFormatPrimaryVolumeDescriptor extends AbstractVolumeDescriptor {
    // 0 - Byte in descriptor
    private final byte volumeDescriptorType;
    // 1-5
    private final byte[] standardIdentifier;
    // 6
    private final byte volumeDescriptorVersion;
    // 7 - Unused 1 byte
    // 8-39 - System Identifier, a-characters
    private final byte[] systemIdentifier;
    // 40-71 - Volume Identifier, d-characters
    private final byte[] volumeIdentifier;
    // 72-79 - Unused
    // 80-87 - Volume Space Size
    private final byte[] volumeSpaceSize;
    // 88-119 - Unused
    // 120-123 - Volume Set Size
    private final byte[] volumeSetSize;
    // 124-127
    private final byte[] volumeSequenceNumber;
    // 128-131 - Logical block size
    private final byte[] logicBlockSize;
    // 132-139 - Path Table Size
    private final byte[] pathTableSize;
    // 140-143
    private final byte[] locOfLPathTable;
    // 144-147
    private final byte[] locOfOptionalLPathTable;
    // 148-151
    private final byte[] locOfMPathTable;
    // 152-155
    private final byte[] locOfOptionalMPathTable;
    // 156-189
    private final byte[] directoryRecordForRootDirectory;
    // 190-317 - Volume Set identifier, d-characters
    private final byte[] volumeSetIdentifier;
    // 318-445 - Publisher Identifier, a-charters
    private final byte[] publisherIdentifier;
    // 446-573 - Data Preparer Identifier, a-characters
    private final byte[] dataPreparerIdentifier;
    // 574-701 - ApplicationIdentifier, a-characters
    private final byte[] applicationIdentifier;
    // 702-738 - Copyright File Identifier, d-characters, separator 1, separator 2
    private final byte[] copyrightFileIdentifier;
    // 739-775 - Abstract File Identifier, d-characters, separator 1, separator 2
    private final byte[] abstractFileIdentifier;
    // 776-812 - Bibliographic File Identifier, d-characters, separator 1, separator 2
    private final byte[] bibliographicFileIdentifier;
    // 813-829
    private final byte[] volumeCreationDataTime;
    // 831-846
    private final byte[] volumeModificationDateTime;
    // 847-863
    private final byte[] volumeExpirationDateTime;
    // 864-880
    private final byte[] volumeEffectiveDateTime;
    // 881
    private final byte fileStructureVersion;
    // 882 - Reserved for future use
    // 883-1394 - Application Use
    // 1395-2047 - Reserved for future use

    public IsoFormatPrimaryVolumeDescriptor(byte[] header) {
        super(header);
        volumeDescriptorType = header[0];
        standardIdentifier = Arrays.copyOfRange(header, 1, 6);
        volumeDescriptorVersion = header[6];

        // 8-39 - System Identifier, a-characters
        systemIdentifier = Arrays.copyOfRange(header, 8, 40);
        // 40-71 - Volume Identifier, d-characters
        volumeIdentifier = Arrays.copyOfRange(header, 40, 72);
        // 80-87 - Volume Space Size
        volumeSpaceSize = Arrays.copyOfRange(header, 80, 88);
        // 120-123 - Volume Set Size
        volumeSetSize = Arrays.copyOfRange(header, 120, 124);
        // 124-127
        volumeSequenceNumber = Arrays.copyOfRange(header, 124, 128);
        // 128-131 - Logical block size
        logicBlockSize = Arrays.copyOfRange(header, 128, 132);
        // 132-139 - Path Table Size
        pathTableSize = Arrays.copyOfRange(header, 132, 140);
        // 140-143
        locOfLPathTable = Arrays.copyOfRange(header, 140, 144);
        // 144-147
        locOfOptionalLPathTable = Arrays.copyOfRange(header, 144, 148);
        // 148-151
        locOfMPathTable = Arrays.copyOfRange(header, 148, 152);
        // 152-155
        locOfOptionalMPathTable = Arrays.copyOfRange(header, 152, 156);
        // 156-189
        directoryRecordForRootDirectory = Arrays.copyOfRange(header, 156, 190);
        // 190-317 - Volume Set identifier, d-characters
        volumeSetIdentifier = Arrays.copyOfRange(header, 190, 318);
        // 318-445 - Publisher Identifier, a-charters
        publisherIdentifier = Arrays.copyOfRange(header, 318, 446);
        // 446-573 - Data Preparer Identifier, a-characters
        dataPreparerIdentifier = Arrays.copyOfRange(header, 446, 574);
        // 574-701 - ApplicationIdentifier, a-characters
        applicationIdentifier = Arrays.copyOfRange(header, 574, 702);
        // 702-738 - Copyright File Identifier, d-characters, separator 1, separator 2
        copyrightFileIdentifier = Arrays.copyOfRange(header, 702, 739);
        // 739-775 - Abstract File Identifier, d-characters, separator 1, separator 2
        abstractFileIdentifier = Arrays.copyOfRange(header, 739, 776);
        // 776-812 - Bibliographic File Identifier, d-characters, separator 1, separator 2
        bibliographicFileIdentifier = Arrays.copyOfRange(header, 776, 813);
        // 813-829
        volumeCreationDataTime = Arrays.copyOfRange(header, 813, 830);
        // 830-846
        volumeModificationDateTime = Arrays.copyOfRange(header, 830, 847);
        // 847-863
        volumeExpirationDateTime = Arrays.copyOfRange(header, 847, 864);
        // 864-880
        volumeEffectiveDateTime = Arrays.copyOfRange(header, 864, 881);
        // 881
        fileStructureVersion = header[881];
        // 882 - Reserved for future use
        // 883-1394 - Application Use
        // 1395-2047 - Reserved for future use
    }

    /**
     * This is always 1 for this type of record.
     *
     * @return byte of volume description type, for this type it should be 1
     */
    @Override
    public byte getVolumeDescriptorType() {
        return volumeDescriptorType;
    }

    /**
     * This is always CD001 according to the standard.
     *
     * @return byte array of standard Identifier
     */
    @Override
    public byte[] getStandardIdentifier() {
        return standardIdentifier;
    }

    /**
     * Get the volume descriptor type as an int.
     *
     * @return int of volume descriptor type
     */
    @Override
    public int getVolumeDescriptorTypeAsInt() {
        return getVolumeDescriptorType();
    }

    /**
     * Get bytes of the volumeDescriptorVersion.
     *
     * @return byte
     */
    @Override
    public byte getVolumeDescriptorVersion() {
        return volumeDescriptorVersion;
    }

    /**
     * Get the volume descriptor type as an int.
     *
     * @return int of the volume descriptor version
     */
    public int getVolumeDescriptorVersionAsInt() {
        return getVolumeDescriptorType();
    }

    // volumeDescriptorVersion = header[7]; always 1, not creating a getter

    /**
     * Get the identifier for the system that created the image.
     *
     * @return bytes 32 of the data
     */
    public byte[] getSystemIdentifier() {
        return systemIdentifier;
    }

    /**
     * The identifier is a String, allow the user to get the String if wanted.
     *
     * @return String of Identifier
     */
    public String getSystemIdentifierAsString() {
        return new String(getSystemIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Volume Identifier in bytes.
     *
     * @return volume id
     */
    public byte[] getVolumeIdentifier() {
        return volumeIdentifier;
    }

    /**
     * Volume Identifier as String.
     *
     * @return string of Volume ID
     */
    public String getVolumeIdentifierAsString() {
        return new String(getVolumeIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Get volume space size, number of logical sectors on the volume in byte array. This is in "both byte order".
     * Standard 7.3.3, both byte order (yz wx uv st st uv wx yz)
     *
     * @return bytes of number of logical sectors
     */
    public byte[] getVolumeSpaceSize() {
        return volumeSpaceSize;
    }

    /**
     * Get volume space size, number of logical sectors on the volume as a long. Standard 7.3.3, both byte order (yz wx
     * uv st st uv wx yz)
     *
     * @return Long of number of logical sectors
     */
    public long getVolumeSpaceSizeAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getVolumeSpaceSize(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get the number of discs in this volume set.
     *
     * @return byte of the number of discs in volume
     */
    public byte[] getVolumeSetSize() {
        return volumeSetSize;
    }

    /**
     * Get the number of discs in this volume set.
     *
     * @return int of the number of discs in volume
     */
    public int getVolumeSetSizeAsInt() {
        byte[] paddedFixedBothNumber = new byte[4];
        System.arraycopy(Arrays.copyOfRange(getVolumeSetSize(), 2, 4), 0, paddedFixedBothNumber, 2, 2);
        return ByteBuffer.wrap(paddedFixedBothNumber).getInt();
    }

    /**
     * Get which image this is in a sequence of images.
     *
     * @return byte of the disc number in volume
     */
    public byte[] getVolumeSequenceNumber() {
        return volumeSequenceNumber;
    }

    /**
     * Get which disc this is in a volume.
     *
     * @return int of the disc number in volume
     */
    public int getVolumeSequenceNumberAsInt() {
        byte[] paddedFixedBothNumber = new byte[4];
        System.arraycopy(Arrays.copyOfRange(getVolumeSequenceNumber(), 2, 4), 0, paddedFixedBothNumber, 2, 2);
        return ByteBuffer.wrap(paddedFixedBothNumber).getInt();
    }

    /**
     * Bytes of the logical block size, usually this is 2048.
     *
     * @return byte array of logical block size
     */
    public byte[] getLogicBlockSize() {
        return logicBlockSize;
    }

    /**
     * Int of the logical block size, usually this is 2048.
     *
     * @return int array of logical block size
     */
    public int getLogicBlockSizeAsInt() {
        byte[] paddedFixedBothNumber = new byte[4];
        System.arraycopy(Arrays.copyOfRange(getLogicBlockSize(), 2, 4), 0, paddedFixedBothNumber, 2, 2);
        return ByteBuffer.wrap(paddedFixedBothNumber).getInt();
    }

    /**
     * Get the size of the Path table as byte array.
     *
     * @return bytes of number of logical sectors in
     */
    public byte[] getPathTableSize() {
        return pathTableSize;
    }

    /**
     * Get the size of the Path table as long. Standard 7.3.3, both byte order (yz wx uv st st uv wx yz)
     *
     * @return long of number of logical sectors in
     */
    public long getPathTableSizeAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getPathTableSize(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Bytes of loc of L path table, note this is LEAST SIGNIFICANT FIRST. (yz wx uv st)
     *
     * @return byte array of loc
     */
    public byte[] getLocOfLPathTable() {
        return locOfLPathTable;
    }

    /**
     * Get the location of the L Path table as long. Standard 7.3.1, least significant first 32
     *
     * @return long of number of logical sectors in
     */
    public long getLPathTableLocAsLong() {
        byte[] paddedFixedBothNumber = Util.convertFromLeastSignificantAndPad(getLocOfLPathTable(), 4, (byte) 0);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get the location of the L Optional Path table as byte array. Standard 7.3.1, least significant first 32
     *
     * @return bytes of number of logical sectors in
     */
    public byte[] getLocOfOptionalLPathTable() {
        return locOfOptionalLPathTable;
    }

    /**
     * Get the location of the L Optional Path table as long. Standard 7.3.1, least significant first 32
     *
     * @return long of number of logical sectors in
     */
    public long getLocOfOptionalLPathTableAsLong() {
        byte[] paddedFixedBothNumber =
                Util.convertFromLeastSignificantAndPad(getLocOfOptionalLPathTable(), 4, (byte) 0);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get the location of the M Path table as bytes. Standard 7.3.2, most significant first 32
     *
     * @return bytes of number of logical sectors in
     */
    public byte[] getLocOfMPathTable() {
        return locOfMPathTable;
    }

    /**
     * Get the location of the M Path table as long. Standard 7.3.2, most significant first 32
     *
     * @return long of number of logical sectors in
     */
    public long getLocOfMPathTableAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(getLocOfMPathTable(), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get the location of the M Optional Path table as bytes. Standard 7.3.2, most significant first 32
     *
     * @return bytes of number of logical sectors in
     */
    public byte[] getLocOfOptionalMPathTable() {
        return locOfOptionalMPathTable;
    }

    /**
     * Get the location of the M Optional Path table as long. Standard 7.3.2, most significant first 32
     *
     * @return long of number of logical sectors in
     */
    public long getLocOfOptionalMPathTableAsLong() {
        byte[] paddedFixedBothNumber = new byte[8];
        System.arraycopy(Arrays.copyOfRange(getLocOfOptionalMPathTable(), 4, 8), 0, paddedFixedBothNumber, 4, 4);
        return ByteBuffer.wrap(paddedFixedBothNumber).getLong();
    }

    /**
     * Get the byte data of the root directory record.
     *
     * @return byte array of root record
     */
    public byte[] getDirectoryRecordForRootDirectory() {
        return directoryRecordForRootDirectory;
    }

    /**
     * Get the root directory information converted into a IsoDirectoryRecord.
     *
     * @return Structure of the initial record
     */
    public IsoFormatDirectoryRecord getDirectoryRecordForRootDirectoryAsIsoDirectorRecord() {
        return new IsoFormatDirectoryRecord(getDirectoryRecordForRootDirectory(), "");
    }

    /**
     * This value is for images that are in a set of discs. This gets the set identifier as bytes.
     *
     * @return byte array of volume set identifier
     */
    public byte[] getVolumeSetIdentifier() {
        return volumeSetIdentifier;
    }

    /**
     * This value is for images that are in a set of discs. This gets the set identifier a String.
     *
     * @return String of volume set identifier
     */
    public String getVolumeSetIdentifierAsString() {
        return new String(getVolumeSetIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Get a byte array of a UTF-8 String of the publisher of the image.
     *
     * @return byte array of utf-8 data
     */
    public byte[] getPublisherIdentifier() {
        return publisherIdentifier;
    }

    /**
     * Get a String of a UTF-8 byte array of the publisher of the image.
     *
     * @return String of utf-8 data
     */
    public String getPublisherIdentiferAsString() {
        return new String(getPublisherIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * This field notes who the data preparer of the image is. This is the raw UTF-8 bytes.
     *
     * @return byte array of data
     */
    public byte[] getDataPreparerIdentifier() {
        return dataPreparerIdentifier;
    }

    /**
     * This field notes who the data preparer of the image is. This is a string of that data.
     *
     * @return String of data preparer
     */
    public String getDataPreparerIdentifierAsString() {
        // TODO(#): If the first byte is 5F then the rest of the data points to a file
        return new String(getDataPreparerIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * This field notes who the Application of the image is. This is the raw UTF-8 bytes.
     *
     * @return byte array of data
     */
    public byte[] getApplicationIdentifier() {
        return applicationIdentifier;
    }

    /**
     * This field notes who the Application of the image is. This is a string of that data.
     *
     * @return String of data
     */
    public String getApplicationIdentifierAsString() {
        return new String(getApplicationIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Byte of copyright identifier, note this can start with 0x5F and then this is refering to a file on disc.
     *
     * @return byte array of copyright info
     */
    public byte[] getCopyrightFileIdentifier() {
        return copyrightFileIdentifier;
    }

    /**
     * String of copyright identifier, copyright ID which is UTF-8 Converted to string.
     *
     * @return String of copyright info
     */
    public String getCopyrightFileIdentifierAsString() {
        // This should point to a file with copyright info
        return new String(getCopyrightFileIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Identifiers a file that describes volumes in the Volume sequence.
     *
     * @return byte array of file name describing volume set
     */
    public byte[] getAbstractFileIdentifier() {
        return abstractFileIdentifier;
    }

    /**
     * Identifiers a file that describes volumes in the Volume sequence, String representation of UTF-8 data.
     *
     * @return String of file name describing volume set
     */
    public String getAbstractFileIdentifierAsString() {
        return new String(getAbstractFileIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Identifiers a file that describes bibliographic information of the image.
     *
     * @return byte array of UTF-8 data of filename
     */
    public byte[] getBibliographicFileIdentifier() {
        return bibliographicFileIdentifier;
    }

    /**
     * Identifiers a file that describes bibliographic information of the image.
     *
     * @return String of filename
     */
    public String getBibliographicFileIdentifierAsString() {
        return new String(getBibliographicFileIdentifier(), StandardCharsets.UTF_8);
    }

    /**
     * Bytes describing the volume creation data according to ECMA-119 8.4.26.1.
     *
     * @return byte array
     */
    public byte[] getVolumeCreationDataTime() {
        return volumeCreationDataTime;
    }

    /**
     * Date object describing the volume creation data according to ECMA-119 8.4.26.1.
     *
     * @return converted data object
     */
    public Optional<Date> getVolumeCreationDataTimeAsDate() throws ParseException {
        return Util.convert8_4_26_1DateTime(getVolumeCreationDataTime());
    }

    /**
     * Byte array of the volume modification date and time.
     *
     * @return raw bytes array
     */
    public byte[] getVolumeModificationDateTime() {
        return volumeModificationDateTime;
    }

    /**
     * Date object describing the volume modification data according to ECMA-119 8.4.26.1.
     *
     * @return converted data object
     */
    public Optional<Date> getVolumeModificationDateTimeAsDate() throws ParseException {
        return Util.convert8_4_26_1DateTime(getVolumeModificationDateTime());
    }

    /**
     * Byte array of the volume expiration date and time.
     *
     * @return raw bytes array
     */
    public byte[] getVolumeExpirationDateTime() {
        return volumeExpirationDateTime;
    }

    /**
     * Date object describing the volume expiration data according to ECMA-119 8.4.26.1.
     *
     * @return converted data object
     */
    public Optional<Date> getVolumeExpirationDateTimeAsDate() throws ParseException {
        return Util.convert8_4_26_1DateTime(getVolumeExpirationDateTime());
    }

    /**
     * Byte array of the volume effective date and time.
     *
     * @return raw bytes array
     */
    public byte[] getVolumeEffectiveDateTime() {
        return volumeEffectiveDateTime;
    }

    /**
     * Date object describing the volume effective data according to ECMA-119 8.4.26.1.
     *
     * @return converted data object
     */
    public Optional<Date> getVolumeEffectiveDateTimeAsDate() throws ParseException {
        return Util.convert8_4_26_1DateTime(getVolumeEffectiveDateTime());
    }

    /**
     * Get the version of file structure in a single byte.
     *
     * @return byte of version
     */
    public byte getFileStructureVersion() {
        return fileStructureVersion;
    }

    /**
     * File structure version converted into an int.
     *
     * @return int of version
     */
    public int getFileStructureVersionAsInt() {
        return getFileStructureVersion();
    }

    /**
     * Checking the header to have the standard specified markers.
     *
     * @param header 2048 of bytes from the header of the disk, usually 2048 * 16 bytes into the disc
     * @return true or false for a valid header
     */
    public static boolean validator(byte[] header) {
        // Minimal size
        if (header.length < 882) {
            return false;
        }

        // volumeDescriptorType
        if (header[0] != 0x01) {
            return false;
        }

        // standardIdentifier
        if (!Arrays.equals("CD001".getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(header, 1, 6))) {
            return false;
        }

        // volumeDescriptorVersion
        if (header[6] != 0x01) {
            return false;
        }

        // Unused space should be 0
        if (header[7] != 0x00) {
            return false;
        }

        // TODO(#1): Check other spaces that are supposed ot be blank
        return true;
    }
}

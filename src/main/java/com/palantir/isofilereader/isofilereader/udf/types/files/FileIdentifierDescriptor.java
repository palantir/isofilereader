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

package com.palantir.isofilereader.isofilereader.udf.types.files;

import com.palantir.isofilereader.isofilereader.Util;
import com.palantir.isofilereader.isofilereader.udf.types.toc.GenericDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.types.LongAd;
import java.util.Arrays;

/**
 * Tag Type 257; max size: Max of logical block size (2048).
 * Section 14.4/Page 4/20 of original spec.
 */
@SuppressWarnings("StrictUnusedVariable")
public class FileIdentifierDescriptor extends GenericDescriptor {
    // struct tag DescriptorTag;
    // Uint16 FileVersionNumber;
    private final byte[] fileVersionNumber;
    // Uint8 FileCharacteristics;
    private final byte fileCharacteristics;
    // Uint8 LengthOfFileIdentifier;
    private final byte lengthOfFileIdentifier;
    // struct long_ad ICB;
    private final LongAd informationControlBlock;
    // Uint16 LengthOfImplementationUse;
    private final byte[] lengthOfImplementationUse;
    // byte ImplementationUse[];
    private final byte[] implementationUse;
    // char FileIdentifier[];
    private final byte[] fileIdentifier;
    // byte Padding[];

    public FileIdentifierDescriptor(byte[] record) {
        // struct tag DescriptorTag; this is 16 bytes
        super(record);
        // Uint16 FileVersionNumber;
        this.fileVersionNumber = Arrays.copyOfRange(record, 16, 18);
        // Uint8 FileCharacteristics;
        this.fileCharacteristics = record[18];
        // Uint8 LengthOfFileIdentifier;
        this.lengthOfFileIdentifier = record[19];
        // struct long_ad ICB;
        this.informationControlBlock = new LongAd(Arrays.copyOfRange(record, 20, 36));
        // Uint16 LengthOfImplementationUse;
        this.lengthOfImplementationUse = Arrays.copyOfRange(record, 36, 38);
        // byte ImplementationUse[];
        this.implementationUse = Arrays.copyOfRange(record, 38, 38 + getLengthOfImplementationUseAsInt());
        // char FileIdentifier[];
        this.fileIdentifier = Arrays.copyOfRange(
                record,
                38 + getLengthOfImplementationUseAsInt(),
                38 + getLengthOfImplementationUseAsInt() + getLengthOfFileIdentifierAsInt());
    }

    /**
     * Get the version of this recorded file.
     *
     * @return byte array
     */
    public byte[] getFileVersionNumber() {
        return fileVersionNumber;
    }

    /**
     * Get the file characteristics. Page 4/22 of original standard has what these flags can mean.
     *
     * @return byte of flags
     */
    public byte getFileCharacteristics() {
        return fileCharacteristics;
    }

    /**
     * Within the File Characteristics, existence is the first bit; 0 means the user should know the file exists, 1
     * means it can be hidden.
     *
     * @return file should be known to the user
     */
    public boolean getFileCharacteristicsExistenceIsSet() {
        return (getFileCharacteristics() & (1L << 0)) == 0;
    }

    /**
     * Within the File Characteristics, Directory is the second bit; 0 it is not a directory, 1 is a directory.
     * Note: if the file is a symbolic link, this should be 0.
     *
     * @return file is a directory
     */
    public boolean getFileCharacteristicsExistenceIsDirectory() {
        return (getFileCharacteristics() & (1L << 1)) != 0;
    }

    /**
     * Within the File Characteristics, deleted is the third bit, 1 if the file was deleted.
     *
     * @return file is deleted
     */
    public boolean getFileCharacteristicsExistenceIsDeleted() {
        return (getFileCharacteristics() & (1L << 2)) != 0;
    }

    /**
     * Within the File Characteristics, Parent is the fourth bit; to quote the original standard.
     * Parent: If set to ONE, shall mean that the ICB field of this descriptor identifies the ICB associated
     * with the file in which is recorded the parent directory of the directory that this descriptor is recorded
     * in; If set to ZERO, shall mean that the ICB field identifies the ICB associated with the file specified
     * by this descriptor
     *
     * @return data is for a parent
     */
    public boolean getFileCharacteristicsExistenceIsParent() {
        return (getFileCharacteristics() & (1L << 3)) != 0;
    }

    /**
     * Within the File Characteristics, metadata is the 5th bit. 0 if the data is not a stream directory, 1 if the
     * data contains implementation use for a stream.
     *
     * @return file a stream
     */
    public boolean getFileCharacteristicsExistenceIsStream() {
        return (getFileCharacteristics() & (1L << 4)) != 0;
    }

    /**
     * Get the length of the file identifier as a byte.
     *
     * @return unsigned byte of the length
     */
    public byte getLengthOfFileIdentifier() {
        return lengthOfFileIdentifier;
    }

    /**
     * Convert the byte of File Identifier length to a Java int.
     *
     * @return int of length
     */
    public int getLengthOfFileIdentifierAsInt() {
        return Byte.toUnsignedInt(getLengthOfFileIdentifier());
    }

    /**
     * Get the Icb for this file identifier.
     *
     * @return LongAd format icb
     */
    public LongAd getInformationControlBlock() {
        return informationControlBlock;
    }

    /**
     * Byte array of the length of the implementation use data.
     *
     * @return byte array
     */
    public byte[] getLengthOfImplementationUse() {
        return lengthOfImplementationUse;
    }

    /**
     * Java int converted for the implementation use length.
     *
     * @return int of length
     */
    public int getLengthOfImplementationUseAsInt() {
        return Util.twoUnsignedByteToInt(getLengthOfImplementationUse());
    }

    /**
     * Get the implementation use data. This data is a regid, and use of that is recorded on 4/22 of the original
     * standard.
     *
     * @return byte array
     */
    public byte[] getImplementationUse() {
        return implementationUse;
    }

    /**
     * Get the file identifier as a byte array.
     *
     * @return byte array
     */
    public byte[] getFileIdentifier() {
        return fileIdentifier;
    }
}

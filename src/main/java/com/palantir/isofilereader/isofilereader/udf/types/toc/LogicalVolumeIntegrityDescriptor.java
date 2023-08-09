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

package com.palantir.isofilereader.isofilereader.udf.types.toc;

import com.palantir.isofilereader.isofilereader.Util;
import com.palantir.isofilereader.isofilereader.udf.types.types.ExtendedDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.types.Timestamp;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tag Type 9; max size: no max.
 */
public class LogicalVolumeIntegrityDescriptor extends GenericDescriptor {
    // struct tag DescriptorTag,
    // Timestamp RecordingDateAndTime,
    private final Timestamp recordingDateAndTime;
    // Uint32 IntegrityType,
    private final byte[] integrityType;
    // struct extent_ad NextIntegrityExtent,
    private final ExtendedDescriptor nextIntegrityExtent;
    // byte LogicalVolumeContentsUse[32],
    private final byte[] logicalVolumeContentsUse;
    // Uint32 NumberOfPartitions,
    private final byte[] numberOfPartitions;
    // Uint32 LengthOfImplementationUse, /* = L_IU */
    private final byte[] lengthOfImplementationUse;
    // Uint32 FreeSpaceTable[], times how many partitions there are
    private List<byte[]> freeSpaceTables = new ArrayList<byte[]>();
    // Uint32 SizeTable[], times how many partitions there are
    private List<byte[]> sizeTables = new ArrayList<>();
    // byte ImplementationUse[]
    private final byte[] implementationUse;

    public LogicalVolumeIntegrityDescriptor(byte[] record) {
        super(record);
        // Timestamp RecordingDateAndTime,
        this.recordingDateAndTime = new Timestamp(Arrays.copyOfRange(record, 16, 28));
        // Uint32 IntegrityType,
        this.integrityType = Arrays.copyOfRange(record, 28, 32);
        // struct extent_ad NextIntegrityExtent,
        this.nextIntegrityExtent = new ExtendedDescriptor(Arrays.copyOfRange(record, 32, 40));
        // byte LogicalVolumeContentsUse[32],
        this.logicalVolumeContentsUse = Arrays.copyOfRange(record, 40, 72);
        // Uint32 NumberOfPartitions,
        this.numberOfPartitions = Arrays.copyOfRange(record, 72, 76);
        // Uint32 LengthOfImplementationUse, /* = L_IU */
        this.lengthOfImplementationUse = Arrays.copyOfRange(record, 76, 80);
        // The FreeSpaceTable and Size Tables are per Partition
        int start = 80;
        for (int i = 0; i < getNumberOfPartitionsAsInt(); i++) {
            // Uint32 FreeSpaceTable[]
            byte[] tempFreeSpaceTable = Arrays.copyOfRange(record, start + (i * 4), start + 4 + (i * 4));
            freeSpaceTables.add(tempFreeSpaceTable);
            // Uint32 SizeTable[]
            byte[] tempSizeTable = Arrays.copyOfRange(record, start + 4 + (i * 4), start + 8 + (i * 4));
            sizeTables.add(tempSizeTable);
            start += 8;
        }
        // byte ImplementationUse[]
        this.implementationUse = Arrays.copyOfRange(record, start, start + getLengthOfImplementationUseAsInt());
        /*
           The ImplementationUse area for the Logical Volume Integrity Descriptor shall be
               structured as follows:
               ImplementationUse format
               RBP Length Name Contents
               0 32 ImplementationID EntityID
               32 4 Number of Files Uint32
               36 4 Number of Directories Uint32
               40 2 Minimum UDF Read Revision Uint16
               42 2 Minimum UDF Write Revision Uint16
               44 2 Maximum UDF Write Revision Uint16
               46 L_IU - 46 Implementation Use byte
        */
    }

    /**
     * Timestamp of image recording data and time.
     *
     * @return timestamp
     */
    public Timestamp getRecordingDateAndTime() {
        return recordingDateAndTime;
    }

    /**
     * Byte array of integrity type setting.
     *
     * @return byte array
     */
    public byte[] getIntegrityType() {
        return integrityType;
    }

    /**
     * Extended Descriptor (extent_ad) of the next Integrity info. This is needed to also get the UDF revision.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getNextIntegrityExtent() {
        return nextIntegrityExtent;
    }

    /**
     * LogicalVolumeContentUse is either the Logical Volume Header descriptor or first of the File Set Descriptor
     * Sequence depending on where in the standard you are. Page 4/1, section 3.1 in the 1997 origianl UDF spec.
     *
     * @return byte array
     */
    public byte[] getLogicalVolumeContentsUse() {
        return logicalVolumeContentsUse;
    }

    /**
     * Raw byte array of number of partitions.
     *
     * @return byte array
     */
    public byte[] getNumberOfPartitions() {
        return numberOfPartitions;
    }

    /**
     * Get number of partitions, this is important because this decides the size of the freespace and size table arrays.
     *
     * @return int of number
     */
    public int getNumberOfPartitionsAsInt() {
        return Util.fourUnsignedByteToInt(getNumberOfPartitions());
    }

    /**
     * Byte array of the implementation use data. This holds information like number of files, folders, and UDF
     * revision.
     *
     * @return byte array
     */
    public byte[] getLengthOfImplementationUse() {
        return lengthOfImplementationUse;
    }

    /**
     * Get the length as an int, of the implementation use field.
     *
     * @return int of length
     */
    public int getLengthOfImplementationUseAsInt() {
        return Util.fourUnsignedByteToInt(getLengthOfImplementationUse());
    }

    /**
     * Get the free space tables arrays, one item per partition, each is 4 bytes.
     *
     * @return List of byte arrays, 4 byte each
     */
    public List<byte[]> getFreeSpaceTables() {
        return freeSpaceTables;
    }

    /**
     * Set the free space tables arrays, one item per partition, each is 4 bytes.
     *
     * @param freeSpaceTables size table
     */
    public void setFreeSpaceTables(List<byte[]> freeSpaceTables) {
        this.freeSpaceTables = freeSpaceTables;
    }

    /**
     * Get the size tables arrays, one item per partition, each is 4 bytes.
     *
     * @return List of byte arrays, 4 byte each
     */
    public List<byte[]> getSizeTables() {
        return sizeTables;
    }

    /**
     * Set the size tables arrays, one item per partition, each is 4 bytes.
     *
     * @param sizeTables size table
     */
    public void setSizeTables(List<byte[]> sizeTables) {
        this.sizeTables = sizeTables;
    }

    /**
     * UDF 2.60 2.2.6.4 spec goes over what should be here.
     *
     * @return byte array of raw ImplementationUse
     */
    public byte[] getImplementationUse() {
        return implementationUse;
    }

    /**
     * Get a string of which software implemented this image from the implementation use field.
     *
     * @return string of the implementor
     */
    public String getImplementationUseImplementationId() {
        return new String(Arrays.copyOfRange(getImplementationUse(), 0, 32), StandardCharsets.UTF_8);
    }

    /**
     * Get the number of files recorded from the implementation use data.
     *
     * @return int of number of files
     */
    public int getImplementationUseNumberOfFiles() {
        final ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(getImplementationUse(), 32, 36));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the number of directories recorded from the implementation use data.
     *
     * @return int of number of directories
     */
    public int getImplementationUseNumberOfDirectories() {
        final ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(getImplementationUse(), 36, 40));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the Min UDF Read Revision out of the Implementation use Data and convert to a string like "2.60".
     *
     * @return string of UDF revision
     */
    public String getImplementationUseMinUdfReadRevision() {
        return getImplementationUse()[40] + "." + getImplementationUse()[41];
    }

    /**
     * Get the Min UDF Writing Revision out of the Implementation use Data and convert to a string like "2.60".
     *
     * @return string of UDF revision
     */
    public String getImplementationUseMinUdfWriteRevision() {
        return getImplementationUse()[42] + "." + getImplementationUse()[43];
    }

    /**
     * Get the Max UDF Writing Revision out of the Implementation use Data and convert to a string like "2.60".
     *
     * @return string of UDF revision
     */
    public String getImplementationUseMaxUdfWriteRevision() {
        return getImplementationUse()[44] + "." + getImplementationUse()[45];
    }
}

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
import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import com.palantir.isofilereader.isofilereader.udf.types.types.IcbTag;
import com.palantir.isofilereader.isofilereader.udf.types.types.LongAd;
import com.palantir.isofilereader.isofilereader.udf.types.types.Timestamp;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Tag Type 261; max size: Max of logical block size (2048).
 * Section 14.9/Page 4/27 of original document.
 * Page 62 of UDF 2.60 doc.
 */
@SuppressWarnings("StrictUnusedVariable")
public class FileEntry extends GenericDescriptor {
    public static final byte NOT_SPECIFIED = 0x0;
    public static final byte UNALLOCATED_SPACE = 0x1;
    public static final byte PARTITION_INTEGRITY_ENTRY = 0x2;
    public static final byte INDIRECT_ENTRY = 0x3;
    public static final byte FOLDER = 0x4;
    public static final byte FILE_AS_RAN_ACCESS_STREAM = 0x5;
    public static final byte BLOCK_DEVICE = 0x6;
    public static final byte CHARACTER_SPECIAL_DEVICE = 0x7;
    public static final byte RECORDED_EXTENDED_ATTRIBUTES = 0x8;
    public static final byte FIFO = 0x9;
    public static final byte C_ISSOCK = 0x10;
    public static final byte TERMINAL_ENTRY = 0x11;
    public static final byte SYMBOLIC_LINK = 0x12;
    public static final byte STREAM_DIRECTORY = 0x13;

    public static final byte METADATA_FILE_MAIN = (byte) 0x250;
    public static final byte METADATA_FILE_MIRROR = (byte) 0x250;

    // struct tag DescriptorTag;
    // struct icbtag ICBTag;
    private final IcbTag icbTag;
    // Uint32 Uid;
    private final byte[] uid;
    // Uint32 Gid;
    private final byte[] gid;
    // Uint32 Permissions;
    private final byte[] permissions;
    // Uint16 FileLinkCount;
    private final byte[] fileLinkCount;
    // Uint8 RecordFormat;
    private final byte recordFormat;
    // Uint8 RecordDisplayAttributes;
    private final byte recordDisplayAttributes;
    // Uint32 RecordLength;
    private final byte[] recordLength;
    // Uint64 InformationLength;
    private final byte[] infoLength;
    // Uint64 LogicalBlocksRecorded;
    private final byte[] logicalBlocksRecorded;
    // struct timestamp AccessTime;
    private final Timestamp accessTime;
    // struct timestamp ModificationTime;
    private final Timestamp modificationTime;
    // struct timestamp AttributeTime;
    private final Timestamp attributeTime;
    // Uint32 Checkpoint;
    private final byte[] checkpoint;
    // struct long_ad ExtendedAttributeICB;
    private final LongAd extendedAttributeIcb;
    // struct EntityID ImplementationIdentifier;
    private final EntityId implementationIdentifier;
    // Uint64 UniqueID,
    private final byte[] uniqueId;
    // Uint32 LengthOfExtendedAttributes;
    private final byte[] lengthOfExtendedAttributes;
    // Uint32 LengthOfAllocationDescriptors;
    private final byte[] lengthOfAllocationDescriptors;
    // byte ExtendedAttributes[];
    private final byte[] extendedAttributes;
    // byte AllocationDescriptors[];
    private final byte[] allocationDescriptors;

    // Extended File Entries only have a few different fields
    private final boolean isExtendedFileEntry;
    private final byte[] extObjectSize;
    private final Timestamp extCreationTime;
    private final LongAd extStreamDirectory;

    public FileEntry(byte[] record) {
        // struct tag DescriptorTag;
        super(record);
        // struct icbtag ICBTag;
        this.icbTag = new IcbTag(Arrays.copyOfRange(record, 16, 36));
        // Uint32 Uid;
        this.uid = Arrays.copyOfRange(record, 36, 40);
        // Uint32 Gid;
        this.gid = Arrays.copyOfRange(record, 40, 44);
        // Uint32 Permissions;
        this.permissions = Arrays.copyOfRange(record, 44, 48);
        // Uint16 FileLinkCount;
        this.fileLinkCount = Arrays.copyOfRange(record, 48, 50);
        // Uint8 RecordFormat;
        this.recordFormat = record[50];
        // Uint8 RecordDisplayAttributes;
        this.recordDisplayAttributes = record[51];
        // Uint32 RecordLength;
        this.recordLength = Arrays.copyOfRange(record, 52, 56);
        // Uint64 InformationLength;
        this.infoLength = Arrays.copyOfRange(record, 56, 64);

        if (super.getDescriptorTag().getTagIdentifierAsInt() == 266) {
            this.isExtendedFileEntry = true;
            // Uint64 ObjectSize
            this.extObjectSize = Arrays.copyOfRange(record, 64, 72);
            // Uint64 LogicalBlocksRecorded;
            this.logicalBlocksRecorded = Arrays.copyOfRange(record, 72, 80);
            // struct timestamp AccessTime;
            this.accessTime = new Timestamp(Arrays.copyOfRange(record, 80, 92));
            // struct timestamp ModificationTime;
            this.modificationTime = new Timestamp(Arrays.copyOfRange(record, 92, 104));
            // struct timestamp CreationTime;
            this.extCreationTime = new Timestamp(Arrays.copyOfRange(record, 104, 116));
            // struct timestamp AttributeTime;
            this.attributeTime = new Timestamp(Arrays.copyOfRange(record, 116, 128));
            // Uint32 Checkpoint;
            this.checkpoint = Arrays.copyOfRange(record, 128, 132);
            // 4 reserved bytes that should be 00
            // struct long_ad ExtendedAttributeICB;
            this.extendedAttributeIcb = new LongAd(Arrays.copyOfRange(record, 136, 152));
            // struct long_ad StreamDirectoryICB
            this.extStreamDirectory = new LongAd(Arrays.copyOfRange(record, 152, 168));
            // struct EntityID ImplementationIdentifier;
            this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 168, 200));
            // Uint64 UniqueID,
            this.uniqueId = Arrays.copyOfRange(record, 200, 208);
            // Uint32 LengthOfExtendedAttributes;
            this.lengthOfExtendedAttributes = Arrays.copyOfRange(record, 208, 212);
            // Uint32 LengthOfAllocationDescriptors;
            this.lengthOfAllocationDescriptors = Arrays.copyOfRange(record, 212, 216);
            // byte ExtendedAttributes[];
            this.extendedAttributes = Arrays.copyOfRange(record, 216, 216 + getLengthOfExtendedAttributesAsInt());
            // byte AllocationDescriptors[];
            this.allocationDescriptors = Arrays.copyOfRange(
                    record,
                    216 + getLengthOfExtendedAttributesAsInt(),
                    216 + getLengthOfExtendedAttributesAsInt() + getLengthOfAllocationDescriptorsAsInt());
        } else {
            this.isExtendedFileEntry = false;
            this.extObjectSize = null;
            this.extCreationTime = null;
            this.extStreamDirectory = null;

            // Uint64 LogicalBlocksRecorded;
            this.logicalBlocksRecorded = Arrays.copyOfRange(record, 64, 72);
            // struct timestamp AccessTime;
            this.accessTime = new Timestamp(Arrays.copyOfRange(record, 72, 84));
            // struct timestamp ModificationTime;
            this.modificationTime = new Timestamp(Arrays.copyOfRange(record, 84, 96));
            // struct timestamp AttributeTime;
            this.attributeTime = new Timestamp(Arrays.copyOfRange(record, 96, 108));
            // Uint32 Checkpoint;
            this.checkpoint = Arrays.copyOfRange(record, 108, 112);
            // struct long_ad ExtendedAttributeICB;
            this.extendedAttributeIcb = new LongAd(Arrays.copyOfRange(record, 112, 128));
            // struct EntityID ImplementationIdentifier;
            this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 128, 160));
            // Uint64 UniqueID,
            this.uniqueId = Arrays.copyOfRange(record, 160, 168);
            // Uint32 LengthOfExtendedAttributes;
            this.lengthOfExtendedAttributes = Arrays.copyOfRange(record, 168, 172);
            // Uint32 LengthOfAllocationDescriptors;
            this.lengthOfAllocationDescriptors = Arrays.copyOfRange(record, 172, 176);
            // byte ExtendedAttributes[];
            this.extendedAttributes = Arrays.copyOfRange(record, 176, 176 + getLengthOfExtendedAttributesAsInt());
            // byte AllocationDescriptors[];
            this.allocationDescriptors = Arrays.copyOfRange(
                    record,
                    176 + getLengthOfExtendedAttributesAsInt(),
                    176 + getLengthOfExtendedAttributesAsInt() + getLengthOfAllocationDescriptorsAsInt());
        }
    }

    /**
     * Get the tag about the File Entry itself.
     *
     * @return IcbTag
     */
    public IcbTag getIcbTag() {
        return icbTag;
    }

    /**
     * Get the file entries User ID, this is used for permission on a Unix System.
     *
     * @return byte array
     */
    public byte[] getUid() {
        return uid;
    }

    /**
     * Get the user id as a java int. This is commonly set to -1 for unspecified, most systems will then apply the
     * user who mounted the image instead.
     *
     * @return user id
     */
    public int getUidAsInt() {
        return Util.fourUnsignedByteToInt(getUid());
    }

    /**
     * Get the file entries Group ID, this is used for permission on a Unix System.
     *
     * @return byte array
     */
    public byte[] getGid() {
        return gid;
    }

    /**
     * Get the group id as a java int. This is commonly set to -1 for unspecified, most systems will then apply the
     * group of the user who mounted the image instead.
     *
     * @return group id
     */
    public int getGidAsInt() {
        return Util.fourUnsignedByteToInt(getGid());
    }

    /**
     * Byte array of the file permissions.
     *
     * @return byte array
     */
    public byte[] getPermissions() {
        return permissions;
    }

    /**
     * Get this libraries class of file permissions.
     *
     * @return FilePerms
     */
    public FilePerms getFilePerms() {
        return new FilePerms(Util.fourUnsignedByteToInt(getPermissions()));
    }

    /**
     * This field specifies the number of File Identifier Descriptors in this ICB.
     *
     * @return byte array
     */
    public byte[] getFileLinkCount() {
        return fileLinkCount;
    }

    // Page 4/30 of original UDF standard goes over these
    public static final int RECORD_FORMAT_NOT_SPECIFIED = 0;
    public static final int RECORD_FORMAT_PADDED_FIXED_LENGTH = 1;
    public static final int RECORD_FORMAT_FIXED_LENGTH = 2;
    public static final int RECORD_FORMAT_VARIABLE_LENGTH_8_RECORDS = 3;
    public static final int RECORD_FORMAT_VARIABLE_LENGTH_16_RECORDS = 4;
    public static final int RECORD_FORMAT_VARIABLE_LENGTH_16_MSB_RECORDS = 5;
    public static final int RECORD_FORMAT_VARIABLE_LENGTH_32_RECORDS = 6;
    public static final int RECORD_FORMAT_STREAM_PRINT = 7;
    public static final int RECORD_FORMAT_STREAM_LF = 8;
    public static final int RECORD_FORMAT_STREAM_CR = 9;
    public static final int RECORD_FORMAT_STREAM_CRLF = 10;
    public static final int RECORD_FORMAT_STREAM_LFCR = 11;

    /**
     * Get a byte representing the record format. The available formats are available as static types of this class.
     *
     * @return byte
     */
    public byte getRecordFormat() {
        return recordFormat;
    }

    /**
     * Byte of the Display Attributes. 0 is display of record not specified. 1 means it matches original UDF standard
     * 5/9.3.1. 2 means it matches original UDF standard 5/9.3.2. 3 means it matches original UDF standard 5/9.3.3.
     *
     * @return byte representation
     */
    public byte getRecordDisplayAttributes() {
        return recordDisplayAttributes;
    }

    /**
     * Get the record length as byte array.
     *
     * @return byte array
     */
    public byte[] getRecordLength() {
        return recordLength;
    }

    /**
     * Get the length of the info as a byte array.
     *
     * @return byte array
     */
    public byte[] getInfoLength() {
        return infoLength;
    }

    /**
     * Convert the byte arrays of info length to a Java long.
     *
     * @return long
     */
    public long getInfoLengthAsLong() {
        final ByteBuffer bb = ByteBuffer.wrap(getInfoLength());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    /**
     * Number of logical blocks recorded for the body of the file.
     *
     * @return byte array
     */
    public byte[] getLogicalBlocksRecorded() {
        return logicalBlocksRecorded;
    }

    /**
     * Get the most recent access time for the file.
     *
     * @return Timestamp
     */
    public Timestamp getAccessTime() {
        return accessTime;
    }

    /**
     * Get a timestamp of the most recent modification time.
     *
     * @return Timestamp
     */
    public Timestamp getModificationTime() {
        return modificationTime;
    }

    /**
     * Timestamp of most recent modification of the attributes of this file.
     *
     * @return Timestamp
     */
    public Timestamp getAttributeTime() {
        return attributeTime;
    }

    /**
     * To quote page 4/32 of the original standard.
     * This field shall contain 1 for the first instance of a file and shall be incremented by 1 when directed to do so
     * by the user. Part 4 does not specify any relationship between the Checkpoint field and the File Version Number
     * field of the directory descriptor identifying the file.
     *
     * @return byte array
     */
    public byte[] getCheckpoint() {
        return checkpoint;
    }

    /**
     * Get a LongAd/ExtendedAttributeIcb of this file.
     *
     * @return LongAd
     */
    public LongAd getExtendedAttributeIcb() {
        return extendedAttributeIcb;
    }

    /**
     * Get a EntityId of the implementation Identifier.
     *
     * @return EntityId
     */
    public EntityId getImplementationIdentifier() {
        return implementationIdentifier;
    }

    /**
     * Get the UID of this file.
     *
     * @return byte array
     */
    public byte[] getUniqueId() {
        return uniqueId;
    }

    /**
     * Get the length of the extended attributes in the file.
     *
     * @return byte array
     */
    public byte[] getLengthOfExtendedAttributes() {
        return lengthOfExtendedAttributes;
    }

    /**
     * Get the length of the extended attributes of the file in a Java int.
     *
     * @return int
     */
    public int getLengthOfExtendedAttributesAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getLengthOfExtendedAttributes());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the length of the Allocation Descriptors.
     *
     * @return byte array
     */
    public byte[] getLengthOfAllocationDescriptors() {
        return lengthOfAllocationDescriptors;
    }

    /**
     * Get the length of the Allocation Descriptors converted to a Java int.
     *
     * @return int of length
     */
    public int getLengthOfAllocationDescriptorsAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(getLengthOfAllocationDescriptors());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Byte array of the extended attributes.
     *
     * @return raw adata in byte array
     */
    public byte[] getExtendedAttributes() {
        return extendedAttributes;
    }

    /**
     * Get data from a short allocation descriptor, EMC 167, Page 4/46 14.14.
     *
     * @return int of length
     */
    public byte[] getAllocationDescriptors() {
        return allocationDescriptors;
    }

    /**
     * Get the length of data stated in the Allocation descriptor as a Java int.
     */
    public int getLengthInAllocationDescriptorAsInt() {
        // Length is overloaded... the most significant 2 bites can mean other things
        /*
            0 Extent recorded and allocated
            1 Extent not recorded but allocated
            2 Extent not recorded and not allocated
            3 The extent is the next extent of allocation descriptors (see 4/12)
        */
        byte[] tempLength = Arrays.copyOfRange(getAllocationDescriptors(), 0, 4);
        // To fix this overload, we will do a bitmask...
        int mask = 63; // 0011 1111 mask is 63
        tempLength[3] = (byte) (tempLength[3] & mask);
        final ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(tempLength, 0, 4));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Get the location stated in the Allocation descriptor as a Java int.
     *
     * @return int of location
     */
    public int getLocationInAllocationDescriptorAsInt() {
        final ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOfRange(getAllocationDescriptors(), 4, 8));
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Return if this is an extended file entry, or a standard one.
     *
     * @return boolean true/false
     */
    public boolean isExtendedFileEntry() {
        return isExtendedFileEntry;
    }

    /**
     * Extended metadata get object size.
     *
     * @return size as bytes
     */
    public byte[] getExtObjectSize() {
        return extObjectSize;
    }

    /**
     * Extended metadata, get creation time.
     *
     * @return timestamp
     */
    public Timestamp getExtCreationTime() {
        return extCreationTime;
    }

    /**
     * Get a LongAd of the stream directory information.
     *
     * @return LongAd
     */
    public LongAd getExtStreamDirectory() {
        return extStreamDirectory;
    }
}

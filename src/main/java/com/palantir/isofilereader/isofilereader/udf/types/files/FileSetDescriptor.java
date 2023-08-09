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

import com.palantir.isofilereader.isofilereader.udf.types.toc.GenericDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.types.CharSpec;
import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import com.palantir.isofilereader.isofilereader.udf.types.types.LongAd;
import com.palantir.isofilereader.isofilereader.udf.types.types.Timestamp;
import java.util.Arrays;

/**
 * Tag Type 256; max size: 512 bytes.
 * Section 14, Page 4/16 of original spec.
 */
@SuppressWarnings("StrictUnusedVariable")
public class FileSetDescriptor extends GenericDescriptor {
    // struct tag DescriptorTag; Done by Abstract class
    // struct timestamp RecordingDateandTime;
    private final Timestamp recordingDateAndTime;
    // Uint16 InterchangeLevel;
    private final byte[] interchangeLevel;
    // Uint16 MaximumInterchangeLevel;
    private final byte[] maximumInterchangeLevel;
    // Uint32 CharacterSetList;
    private final byte[] characterSetList;
    // Uint32 MaximumCharacterSetList;
    private final byte[] maximumCharacterSetList;
    // Uint32 FileSetNumber;
    private final byte[] fileSetNumber;
    // Uint32 FileSetDescriptorNumber;
    private final byte[] fileSetDescriptorNumber;
    // struct charspec LogicalVolumeIdentifierCharacterSet;
    private final CharSpec logicalVolumeIdentifierCharacterSet;
    // dstring LogicalVolumeIdentifier[128];
    private final byte[] logicalVolumeIdentifier;
    // struct charspec FileSetCharacterSet;
    private final CharSpec fileSetCharacterSet;
    // dstring FileSetIdentifier[32];
    private final byte[] fileSetIdentifier;
    // dstring CopyrightFileIdentifier[32];
    private final byte[] copyrightFileIdentifier;
    // dstring AbstractFileIdentifier[32];
    private final byte[] abstractFileIdentifier;
    // struct long_ad RootDirectoryICB;
    private final LongAd rootDirectoryIcb;
    // struct EntityID DomainIdentifier;
    private final EntityId domainIdentifier;
    // struct long_ad NextExtent;
    private final LongAd nextExtent;
    // struct long_ad SystemStreamDirectoryICB;
    private final LongAd systemStreamDirectoryIcb;
    // Reserved 32 bytes

    public FileSetDescriptor(byte[] record) {
        // struct tag DescriptorTag; Done by Abstract class
        super(record);
        // struct timestamp RecordingDateandTime;
        this.recordingDateAndTime = new Timestamp(Arrays.copyOfRange(record, 16, 28));
        // Uint16 InterchangeLevel;
        this.interchangeLevel = Arrays.copyOfRange(record, 28, 30);
        // Uint16 MaximumInterchangeLevel;
        this.maximumInterchangeLevel = Arrays.copyOfRange(record, 30, 32);
        // Uint32 CharacterSetList;
        this.characterSetList = Arrays.copyOfRange(record, 32, 36);
        // Uint32 MaximumCharacterSetList;
        this.maximumCharacterSetList = Arrays.copyOfRange(record, 36, 40);
        // Uint32 FileSetNumber;
        this.fileSetNumber = Arrays.copyOfRange(record, 40, 44);
        // Uint32 FileSetDescriptorNumber;
        this.fileSetDescriptorNumber = Arrays.copyOfRange(record, 44, 48);
        // struct charspec LogicalVolumeIdentifierCharacterSet;
        this.logicalVolumeIdentifierCharacterSet = new CharSpec(Arrays.copyOfRange(record, 48, 112));
        // dstring LogicalVolumeIdentifier[128];
        this.logicalVolumeIdentifier = Arrays.copyOfRange(record, 112, 240);
        // struct charspec FileSetCharacterSet;
        this.fileSetCharacterSet = new CharSpec(Arrays.copyOfRange(record, 240, 304));
        // dstring FileSetIdentifier[32];
        this.fileSetIdentifier = Arrays.copyOfRange(record, 304, 336);
        // dstring CopyrightFileIdentifier[32];
        this.copyrightFileIdentifier = Arrays.copyOfRange(record, 336, 368);
        // dstring AbstractFileIdentifier[32];
        this.abstractFileIdentifier = Arrays.copyOfRange(record, 368, 400);
        // struct long_ad RootDirectoryICB;
        this.rootDirectoryIcb = new LongAd(Arrays.copyOfRange(record, 400, 416));
        // struct EntityID DomainIdentifier;
        this.domainIdentifier = new EntityId(Arrays.copyOfRange(record, 416, 448));
        // struct long_ad NextExtent;
        this.nextExtent = new LongAd(Arrays.copyOfRange(record, 448, 464));
        // struct long_ad SystemStreamDirectoryICB;
        this.systemStreamDirectoryIcb = new LongAd(Arrays.copyOfRange(record, 464, 480));
        // Reserved 32 bytes
    }

    /**
     * Timestamp pf the date and time the information was recorded.
     *
     * @return Timestamp
     */
    public Timestamp getRecordingDateAndTime() {
        return recordingDateAndTime;
    }

    /**
     * Get the level of this interchange of information.
     *
     * @return byte array
     */
    public byte[] getInterchangeLevel() {
        return interchangeLevel;
    }

    /**
     * Maximum supported level of this interchange.
     *
     * @return byte array
     */
    public byte[] getMaximumInterchangeLevel() {
        return maximumInterchangeLevel;
    }

    /**
     * Specified the charspec/Character specification the information in this file set descriptor will be recorded.
     *
     * @return byte array
     */
    public byte[] getCharacterSetList() {
        return characterSetList;
    }

    /**
     * Specified the MAX charspec/Character specification the information in this file set descriptor will be recorded.
     *
     * @return byte array
     */
    public byte[] getMaximumCharacterSetList() {
        return maximumCharacterSetList;
    }

    /**
     * File set number of this descriptor.
     *
     * @return byte array
     */
    public byte[] getFileSetNumber() {
        return fileSetNumber;
    }

    /**
     * File set descriptor number of this file set.
     *
     * @return byte array
     */
    public byte[] getFileSetDescriptorNumber() {
        return fileSetDescriptorNumber;
    }

    /**
     * Get the character set in use for the logical volume identifier.
     *
     * @return CharSpec
     */
    public CharSpec getLogicalVolumeIdentifierCharacterSet() {
        return logicalVolumeIdentifierCharacterSet;
    }

    /**
     * Get a byte array of the logical volume identifier.
     *
     * @return byte array
     */
    public byte[] getLogicalVolumeIdentifier() {
        return logicalVolumeIdentifier;
    }

    /**
     * Get the character set in use for the file set.
     *
     * @return CharSpec
     */
    public CharSpec getFileSetCharacterSet() {
        return fileSetCharacterSet;
    }

    /**
     * Get the file set identifier.
     *
     * @return byte array
     */
    public byte[] getFileSetIdentifier() {
        return fileSetIdentifier;
    }

    /**
     * Get the copyright file identifier.
     *
     * @return byte array
     */
    public byte[] getCopyrightFileIdentifier() {
        return copyrightFileIdentifier;
    }

    /**
     * Get the abstract file identifier.
     *
     * @return byte array
     */
    public byte[] getAbstractFileIdentifier() {
        return abstractFileIdentifier;
    }

    /**
     * Get the root directory Icb, this is the entry point to the file table.
     *
     * @return LongAd
     */
    public LongAd getRootDirectoryIcb() {
        return rootDirectoryIcb;
    }

    /**
     * Get the domain identifier.
     *
     * @return EntityId
     */
    public EntityId getDomainIdentifier() {
        return domainIdentifier;
    }

    /**
     * Get the location of the next extent.
     *
     * @return LongAd
     */
    public LongAd getNextExtent() {
        return nextExtent;
    }

    /**
     * Get the system stream directory Icb if one exists.
     *
     * @return LongAd
     */
    public LongAd getSystemStreamDirectoryIcb() {
        return systemStreamDirectoryIcb;
    }
}

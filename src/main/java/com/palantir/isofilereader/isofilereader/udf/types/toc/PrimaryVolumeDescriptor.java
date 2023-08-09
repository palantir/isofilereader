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
import com.palantir.isofilereader.isofilereader.udf.types.types.CharSpec;
import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import com.palantir.isofilereader.isofilereader.udf.types.types.ExtendedDescriptor;
import com.palantir.isofilereader.isofilereader.udf.types.types.Timestamp;
import java.util.Arrays;

/**
 * Tag Type 1; max size: 512 bytes.
 */
@SuppressWarnings("StrictUnusedVariable")
public class PrimaryVolumeDescriptor extends GenericDescriptor {
    // Uint32 VolumeDescriptorSequenceNumber;
    private final byte[] volumeDescriptorSequenceNumber;
    // Uint32 PrimaryVolumeDescriptorNumber
    private final byte[] primaryVolumeDescriptorNumber;
    // dstring VolumeIdentifier[32]
    private final byte[] volumeIdentifier;
    // Uint16 VolumeSequenceNumber
    private final byte[] volumeSequenceNumber;
    // Uint16 MaximumVolumeSequenceNumber;
    private final byte[] maximumVolumeSequenceNumber;
    // Uint16 InterchangeLevel
    private final byte[] interchangeLevel;
    // Uint16 MaximumInterchangeLevel
    private final byte[] maximumInterchangeLevel;
    // Uint32 CharacterSetList
    private final byte[] characterSetList;
    // Uint32 MaximumCharacterSetList
    private final byte[] maximumCharacterSetList;
    // dstring VolumeSetIdentifier[128]
    private final byte[] volumeSetIdentifier;
    // struct charspec DescriptorCharacterSet
    private final CharSpec descriptorCharacterSet;
    // struct charspec ExplanatoryCharacterSet
    private final CharSpec explanatoryCharacterSet;
    // struct extent_ad VolumeAbstract;
    private final ExtendedDescriptor volumeAbstract;
    // struct extent_ad VolumeCopyrightNotice;
    private final ExtendedDescriptor volumeCopyrightNotice;
    // struct EntityID ApplicationIdentifier;
    private final EntityId applicationIdentifier;
    // struct timestamp RecordingDateandTime;
    private final Timestamp recordingDateandTime;
    // struct EntityID ImplementationIdentifier;
    private final EntityId implementationIdentifier;
    // byte ImplementationUse[64];
    private final byte[] implementationUse;
    // Uint32 PredecessorVolumeDescriptorSequenceLocation;
    private final byte[] predecessorVolumeDescriptorSequenceLocation;
    // Uint16 Flags;
    private final byte[] flags;
    // byte Reserved[22];
    private final byte[] reserved;

    public PrimaryVolumeDescriptor(byte[] record) {
        super(record);
        this.volumeDescriptorSequenceNumber = Arrays.copyOfRange(record, 16, 20);
        this.primaryVolumeDescriptorNumber = Arrays.copyOfRange(record, 20, 24);
        this.volumeIdentifier = Arrays.copyOfRange(record, 24, 56);
        this.volumeSequenceNumber = Arrays.copyOfRange(record, 56, 58);
        this.maximumVolumeSequenceNumber = Arrays.copyOfRange(record, 58, 60);
        this.interchangeLevel = Arrays.copyOfRange(record, 60, 62);
        this.maximumInterchangeLevel = Arrays.copyOfRange(record, 62, 64);
        this.characterSetList = Arrays.copyOfRange(record, 64, 68);
        this.maximumCharacterSetList = Arrays.copyOfRange(record, 68, 72);
        this.volumeSetIdentifier = Arrays.copyOfRange(record, 72, 200);
        this.descriptorCharacterSet = new CharSpec(Arrays.copyOfRange(record, 200, 264));
        this.explanatoryCharacterSet = new CharSpec(Arrays.copyOfRange(record, 264, 328));
        this.volumeAbstract = new ExtendedDescriptor(Arrays.copyOfRange(record, 328, 336));
        this.volumeCopyrightNotice = new ExtendedDescriptor(Arrays.copyOfRange(record, 336, 344));
        this.applicationIdentifier = new EntityId(Arrays.copyOfRange(record, 344, 376));
        this.recordingDateandTime = new Timestamp(Arrays.copyOfRange(record, 376, 388));
        this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 388, 420));
        this.implementationUse = Arrays.copyOfRange(record, 420, 484);
        this.predecessorVolumeDescriptorSequenceLocation = Arrays.copyOfRange(record, 484, 488);
        this.flags = Arrays.copyOfRange(record, 488, 490);
        this.reserved = Arrays.copyOfRange(record, 490, 512);
    }

    /**
     * This Volumes descriptor sequence number.
     *
     * @return Byte Array[4] of the sequence number
     */
    public byte[] getVolumeDescriptorSequenceNumber() {
        return volumeDescriptorSequenceNumber;
    }

    /**
     * This volumes Descriptor number.
     *
     * @return Byte Array[4] of the descriptor number
     */
    public byte[] getPrimaryVolumeDescriptorNumber() {
        return primaryVolumeDescriptorNumber;
    }

    /**
     * The volume identifier as a byte array. This is a dString.
     *
     * @return Byte Array[32] of the volume id
     */
    public byte[] getVolumeIdentifier() {
        return volumeIdentifier;
    }

    /**
     * Same as getVolumeIdentifier() but returning a Java String.
     *
     * @return Java String
     */
    public String getVolumeIdentifierAsString() {
        return Util.convertDStringBytesToString(getVolumeIdentifier());
    }

    /**
     * Get this volumes sequence number.
     *
     * @return Byte Array[2] of the sequence number
     */
    public byte[] getVolumeSequenceNumber() {
        return volumeSequenceNumber;
    }

    /**
     * Maximum number of volumes in this disc sequence.
     *
     * @return Byte Array[2] of the sequence max
     */
    public byte[] getMaximumVolumeSequenceNumber() {
        return maximumVolumeSequenceNumber;
    }

    /**
     * Interchange Level of the volume. This shall be set to 3.
     *
     * @return Byte Array[2] of the interchange level
     */
    public byte[] getInterchangeLevel() {
        return interchangeLevel;
    }

    /**
     * Maximum interchange level of the contents, this shall be set to 3.
     *
     * @return Byte Array[2] of amx interchange level
     */
    public byte[] getMaximumInterchangeLevel() {
        return maximumInterchangeLevel;
    }

    /**
     * List of character sets to use within the contents of the image.
     *
     * @return Byte Array[4] for these character sets
     */
    public byte[] getCharacterSetList() {
        return characterSetList;
    }

    /**
     * The maximum supported character set associated with any file within the image.
     *
     * @return Byte Array[4] of the character sets list
     */
    public byte[] getMaximumCharacterSetList() {
        return maximumCharacterSetList;
    }

    /**
     *The Volume Set Identifier as raw dString.
     *
     * @return Byte Array[128] of the volume set identifier
     */
    public byte[] getVolumeSetIdentifier() {
        return volumeSetIdentifier;
    }

    /**
     * The character set used by the descriptor.
     *
     * @return CharSpec
     */
    public CharSpec getDescriptorCharacterSet() {
        return descriptorCharacterSet;
    }

    /**
     * The character set used by the Explanatory.
     *
     * @return CharSpec
     */
    public CharSpec getExplanatoryCharacterSet() {
        return explanatoryCharacterSet;
    }

    /**
     * Volume descriptor for the Volume Abstract.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getVolumeAbstract() {
        return volumeAbstract;
    }

    /**
     * Volume Copyright Notice.
     *
     * @return ExtendedDescriptor
     */
    public ExtendedDescriptor getVolumeCopyrightNotice() {
        return volumeCopyrightNotice;
    }

    /**
     * Application identifier for what created the image.
     *
     * @return EntityId
     */
    public EntityId getApplicationIdentifier() {
        return applicationIdentifier;
    }

    /**
     * Timestamp for when the image was created.
     *
     * @return Timestaamp object
     */
    public Timestamp getRecordingDateandTime() {
        return recordingDateandTime;
    }

    /**
     * Get the identifier of the implementation.
     *
     * @return EntityId of the implementor
     */
    public EntityId getImplementationIdentifier() {
        return implementationIdentifier;
    }

    /**
     * Implementation Use bytes.
     *
     * @return Byte Array[64] of Implementation Use
     */
    public byte[] getImplementationUse() {
        return implementationUse;
    }

    /**
     * The Predecessor Volume Descriptor Sequence Location.
     *
     * @return Byte Array[4]
     */
    public byte[] getPredecessorVolumeDescriptorSequenceLocation() {
        return predecessorVolumeDescriptorSequenceLocation;
    }

    /**
     * Flags on the volume.
     *
     * @return Byte Array[2]
     */
    public byte[] getFlags() {
        return flags;
    }

    /**
     * These are reserved bytes, really padding for the end of this sector, but accessible if you need them.
     *
     * @return Byte Array[22]
     */
    public byte[] getReserved() {
        return reserved;
    }
}

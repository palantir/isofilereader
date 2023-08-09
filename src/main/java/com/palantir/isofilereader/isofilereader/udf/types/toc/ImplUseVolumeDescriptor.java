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

import com.palantir.isofilereader.isofilereader.udf.types.types.EntityId;
import java.util.Arrays;

/**
 * Tag Type 4; max size: 512 bytes.
 *
 * <a href="http://www.osta.org/specs/pdf/udf260.pdf">UDF Standards Doc, section 2.2.7</a>.
 */
@SuppressWarnings("StrictUnusedVariable")
public class ImplUseVolumeDescriptor extends GenericDescriptor {
    // struct tag DescriptorTag;
    // Uint32 VolumeDescriptorSequenceNumber;
    private final byte[] volumeDescriptorSequenceNumber;

    // struct EntityID ImplementationIdentifier;
    private final EntityId implementationIdentifier;

    // byte ImplementationUse[460];
    private final byte[] implementationUse;

    public ImplUseVolumeDescriptor(byte[] record) {
        super(record);
        this.volumeDescriptorSequenceNumber = Arrays.copyOfRange(record, 16, 20);
        this.implementationIdentifier = new EntityId(Arrays.copyOfRange(record, 20, 52));
        this.implementationUse = Arrays.copyOfRange(record, 52, 512);
    }

    /**
     * Byte array of the volume sequence number for image series with multiple volumes.
     *
     * @return byte array
     */
    public byte[] getVolumeDescriptorSequenceNumber() {
        return volumeDescriptorSequenceNumber;
    }

    /**
     * Identifier for what made the image.
     *
     * @return EntryId
     */
    public EntityId getImplementationIdentifier() {
        return implementationIdentifier;
    }

    /**
     * Implementation use data, which can be checked against standard for advanced use.
     *
     * @return byte array
     */
    public byte[] getImplementationUse() {
        return implementationUse;
    }
}

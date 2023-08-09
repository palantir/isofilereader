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

import com.palantir.isofilereader.isofilereader.udf.types.types.Tag;
import java.util.Arrays;

/**
 * Aka Extended Attribute Header Descriptor.
 * Section 14.10.1/Page 4/32 of original standard.
 * Section 3.3.4.1/Page 73 of UDF 2.60 document.
 */
@SuppressWarnings("StrictUnusedVariable")
public class FileExtendedAttributes {
    // struct tag DescriptorTag;
    private final Tag descriptorTag;

    // Uint32 ImplementationAttributesLocation;
    private final byte[] implementationAttributesLocation;

    // Uint32 ApplicationAttributesLocation;
    private final byte[] applicationAttributesLocation;

    public FileExtendedAttributes(byte[] record) {
        descriptorTag = new Tag(Arrays.copyOfRange(record, 0, 16));
        implementationAttributesLocation = Arrays.copyOfRange(record, 16, 20);
        applicationAttributesLocation = Arrays.copyOfRange(record, 20, 24);
    }
}

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

public final class IsoFormatConstant {
    // This could change one day, but CD-ROM through Bluray are currently set to 2048.
    public static final int BYTES_PER_SECTOR = 2048;
    public static final int BUFFER_SECTORS = 16;
    public static final byte SEPERATOR_1 = 0x2E;
    public static final byte SEPERATOR_2 = 0x3B;

    private IsoFormatConstant() {}
}

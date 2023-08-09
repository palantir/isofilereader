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

package com.palantir.isofilereader.isofilereader.udf.types.types;

/**
 * Page 70, Section 3.3.3.3 of UDF 2.60 document.
 */
public class Permissions {
    /*
     * #define OTHER_Execute 0x00000001
     * #define OTHER_Write 0x00000002
     * #define OTHER_Read 0x00000004
     * #define OTHER_ChAttr 0x00000008
     * #define OTHER_Delete 0x00000010
     * #define GROUP_Execute 0x00000020
     * #define GROUP_Write 0x00000040
     * #define GROUP_Read 0x00000080
     * #define GROUP_ChAttr 0x00000100
     * #define GROUP_Delete 0x00000200
     * #define OWNER_Execute 0x00000400
     * #define OWNER_Write 0x00000800
     * #define OWNER_Read 0x00001000
     * #define OWNER_ChAttr 0x00002000
     * #define OWNER_Delete 0x00004000
     */
}

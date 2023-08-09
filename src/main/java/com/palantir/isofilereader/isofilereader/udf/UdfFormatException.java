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

package com.palantir.isofilereader.isofilereader.udf;

/**
 * A generic UDF error, used when there is an error reading the table of contents in a UDF image (or the UDF reader).
 */
public class UdfFormatException extends Exception {
    /**
     * UDF table of contents errors.
     *
     * @param message message to display to stack trace
     */
    public UdfFormatException(String message) {
        super(message);
    }
}

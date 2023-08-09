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

package com.palantir.isofilereader.isofilereader;

import java.util.Date;
import java.util.Optional;

public abstract class AbstractInternalFile<T> {
    /**
     * This is the parent folder of these children entries.
     *
     * @return sub-folders/sub-files of this folder
     */
    public abstract T[] getChildren();

    /**
     * Add a single child object.
     * @param abstractChild single child to add
     * @return the child with parent attached
     */
    public abstract T addChild(T abstractChild);

    /**
     * Set the sub-entries to this folder.
     *
     * @param abstractInternalFiles are files under this folder (if its a folder)
     */
    public abstract void addChildren(T[] abstractInternalFiles);

    /**
     * Check if this is a folder, false if it's a file.
     *
     * @return is a folder
     */
    public abstract boolean isDirectory();

    /**
     * Get the raw underlying IsoDirectorRecord.
     *
     * @return raw directory entry
     */
    public abstract Object getUnderlyingRecord();

    /**
     * Get the date the file was recorded as a Java Date.
     *
     * @return Date object
     */
    public abstract Optional<Date> getDateAsDate();

    /**
     * Get the filename of the entry.
     *
     * @return string of filename, converted from UTF-8 or UTF-16
     */
    public abstract String getFileName();

    /**
     * Get the full file name with the parent file.
     *
     * @param separatorChar depending on OS you may want to change separator char
     * @return full filename as a string starting with / or \
     */
    public abstract String getFullFileName(char separatorChar);

    /**
     * Get the size of the file.
     *
     * @return long of file size
     */
    public abstract long getSize();

    /**
     * String of parent file name, used to tell full file name and position.
     *
     * @return string with specified file separators of full file name
     */
    public abstract T getParent();

    /**
     * Get the absolute logical sector of the data being represented.
     *
     * @return long of the logical sector, this is usually this x 2048 to get physical location
     */
    public abstract long getLogicalSectorLocation();
}

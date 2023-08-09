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

public class FilePerms {
    // New UDF 2.60 doc, page 70
    // public static final byte[] PERM_OTHER_EXECUTE = {(byte) 0x0, (byte) 0x0, (byte) 0x1};
    public static final int PERM_OTHER_EXECUTE = 1;
    public static final int PERM_OTHER_WRITE = 2;
    public static final int PERM_OTHER_READ = 4;
    // May Change Attributes
    public static final int PERM_OTHER_CHATTR = 8;
    public static final int PERM_OTHER_DELETE = 16;

    public static final int PERM_GROUP_EXECUTE = 32;
    public static final int PERM_GROUP_WRITE = 64;
    public static final int PERM_GROUP_READ = 128;
    public static final int PERM_GROUP_CHATTR = 256;
    public static final int PERM_GROUP_DELETE = 512;

    public static final int PERM_OWNER_EXECUTE = 1024;
    public static final int PERM_OWNER_WRITE = 2048;
    public static final int PERM_OWNER_READ = 4096;
    public static final int PERM_OWNER_CHATTR = 8192;
    public static final int PERM_OWNER_DELETE = 16384;

    private final int setting;

    /**
     * Set this file permissions class.
     *
     * @param data 4 bytes
     */
    public FilePerms(int data) {
        this.setting = data;
    }

    /**
     * Get the other user setting for execute, equal to unix perms ##1.
     *
     * @return boolean for CAN execute
     */
    public boolean canOtherExecute() {
        // With the file mask, this should either come out to some number because it matched, or 0 for false
        return (0 != (setting & PERM_OTHER_EXECUTE));
    }

    /**
     * Get the other user setting for write, equal to unix perms ##2.
     *
     * @return boolean for CAN write
     */
    public boolean canOtherWrite() {
        return (0 != (setting & PERM_OTHER_WRITE));
    }

    /**
     * Get the other user setting for read, equal to unix perms ##4.
     *
     * @return boolean for CAN read
     */
    public boolean canOtherRead() {
        return (0 != (setting & PERM_OTHER_READ));
    }

    /**
     * Get the other user setting for change file attributes.
     *
     * @return boolean for change attributes
     */
    public boolean canOtherChangeAttributes() {
        return (0 != (setting & PERM_OTHER_CHATTR));
    }

    /**
     * Get the other user setting for can delete.
     *
     * @return boolean for CAN delete
     */
    public boolean canOtherDelete() {
        return (0 != (setting & PERM_OTHER_DELETE));
    }

    /**
     * Get the group user setting for execute, equal to unix perms #1#.
     *
     * @return boolean for CAN execute
     */
    public boolean canGroupExecute() {
        // With the file mask, this should either come out to some number because it matched, or 0 for false
        return (0 != (setting & PERM_GROUP_EXECUTE));
    }

    /**
     * Get the Group user setting for write, equal to unix perms #2#.
     *
     * @return boolean for CAN write
     */
    public boolean canGroupWrite() {
        return (0 != (setting & PERM_GROUP_WRITE));
    }

    /**
     * Get the Group user setting for read, equal to unix perms #4#.
     *
     * @return boolean for CAN read
     */
    public boolean canGroupRead() {
        return (0 != (setting & PERM_GROUP_READ));
    }

    /**
     * Get the Group user setting for change file attributes.
     *
     * @return boolean for change attributes
     */
    public boolean canGroupChangeAttributes() {
        return (0 != (setting & PERM_GROUP_CHATTR));
    }

    /**
     * Get the Group user setting for can delete.
     *
     * @return boolean for CAN delete
     */
    public boolean canGroupDelete() {
        return (0 != (setting & PERM_GROUP_DELETE));
    }

    /**
     * Get the User setting for execute, equal to unix perms 1##.
     *
     * @return boolean for CAN execute
     */
    public boolean canUserExecute() {
        // With the file mask, this should either come out to some number because it matched, or 0 for false
        return (0 != (setting & PERM_OWNER_EXECUTE));
    }

    /**
     * Get the User setting for write, equal to unix perms 2##.
     *
     * @return boolean for CAN write
     */
    public boolean canUserWrite() {
        return (0 != (setting & PERM_OWNER_WRITE));
    }

    /**
     * Get the User setting for read, equal to unix perms 4##.
     *
     * @return boolean for CAN read
     */
    public boolean canUserRead() {
        return (0 != (setting & PERM_OWNER_READ));
    }

    /**
     * Get the User setting for change file attributes.
     *
     * @return boolean for change attributes
     */
    public boolean canUserChangeAttributes() {
        return (0 != (setting & PERM_OWNER_CHATTR));
    }

    /**
     * Get the User setting for can delete.
     *
     * @return boolean for CAN delete
     */
    public boolean canUserDelete() {
        return (0 != (setting & PERM_OWNER_DELETE));
    }
}

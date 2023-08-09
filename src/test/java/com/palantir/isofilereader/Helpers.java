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

package com.palantir.isofilereader;

import com.palantir.isofilereader.isofilereader.GenericInternalIsoFile;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import org.junit.jupiter.api.Assertions;

final class Helpers {
    private Helpers() {}

    static String getMd5FromStream(InputStream inputStream) {
        int bufferSize = 1024 * 1024;

        String newlyCreatedMd5 = "";
        try {
            MessageDigest complete = MessageDigest.getInstance("MD5");
            final byte[] data = new byte[bufferSize];
            int count;
            long outerRead = 0;
            while ((count = inputStream.read(data, 0, bufferSize)) != -1) {
                outerRead += count;
                complete.update(data, 0, count);
            }
            System.out.println("Outer Read: " + outerRead);
            BigInteger bigInt = new BigInteger(1, complete.digest());
            StringBuilder hashText = new StringBuilder(bigInt.toString(16));
            // https://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            newlyCreatedMd5 = hashText.toString();
        } catch (Exception e) {
            Assertions.fail(e);
        }
        return newlyCreatedMd5;
    }

    static void treePrint(char sepChar, GenericInternalIsoFile[] records) {
        for (GenericInternalIsoFile singleRecord : records) {
            if (singleRecord.isDirectory() && singleRecord.getChildren() != null) {
                // Directory
                System.out.println(singleRecord.getFullFileName(sepChar));
                treePrint(sepChar, singleRecord.getChildren());
            } else {
                // File
                System.out.println(singleRecord.getFullFileName(sepChar));
            }
        }
    }
}

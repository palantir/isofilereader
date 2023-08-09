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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.palantir.isofilereader.isofilereader.Util;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

public class UtilTest {
    @Test
    void convertFromLeastSignificantAndPad() {
        byte[] testData = {4, 3, 2, 1};
        byte[] wantedResult = {0, 0, 0, 0, 0, 1, 2, 3, 4};
        byte[] result = Util.convertFromLeastSignificantAndPad(testData, 5, (byte) 0);
        assertEquals(0, Arrays.compare(wantedResult, result));
    }

    @Test
    void convert9_1_5DateTime() {
        // 2019/11/21 18:09:17
        byte[] date = {0x77, 0x0B, 0x15, 0x17, 0x09, 0x11, 0x00};
        Optional<Date> time = Util.convert9_1_5DateTime(date);
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MMM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String stringDate = dateFormat.format(time.get());
        assertEquals("2019/Nov/21 23:09:17", stringDate);
        System.out.println(dateFormat.format(time.get()));
    }
}

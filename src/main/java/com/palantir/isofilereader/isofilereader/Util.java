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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

public final class Util {
    private Util() {}

    /**
     * Convert a byte array from the least significant form to most, and then pad as needed. This is used to convert
     * some byte arrays that represent INTs or LONGs in raw ISO records to be easily converted to Java.
     *
     * @param leArray least signicant marked byte array
     * @param padLeftAmount amount of bytes to pad
     * @param paddingValue what value should go in that pad?
     * @return new byte array
     */
    public static byte[] convertFromLeastSignificantAndPad(byte[] leArray, int padLeftAmount, byte paddingValue) {
        byte[] returningArray = new byte[leArray.length + padLeftAmount];
        Arrays.fill(returningArray, paddingValue);
        for (int i = 0; i < leArray.length; i++) {
            returningArray[padLeftAmount + i] = leArray[leArray.length - 1 - i];
        }
        return returningArray;
    }

    /**
     * Convert ISO format dates to Java Date. Note: This hasn't been thoroughly tested, most tools seem to standardize
     * to GMT.
     *
     * @param dateTime byte array of the date, 7 bytes
     * @return Nicely formatted date inside and optional, if the provided data isn't 7 bytes, or cant convert to a
     * date, you will get an empty optional.
     */
    public static Optional<Date> convert9_1_5DateTime(byte[] dateTime) {
        if (dateTime.length != 7) {
            return Optional.empty();
        }
        int year = 1900 + Byte.toUnsignedInt(dateTime[0]);
        int month = dateTime[1] - 1;
        int day = dateTime[2];
        int hour = dateTime[3];
        int minute = dateTime[4];
        int second = dateTime[5];
        int offset = dateTime[6]; // This is a signed number, GMT offset 15 minutes * this
        int rawOffsetAdjustedForJava = offset * 15 * 60 * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        String[] timezones = TimeZone.getAvailableIDs(rawOffsetAdjustedForJava);
        if (timezones.length > 0) { // If an offset is entered and a timezone at that location doesn't exist
            calendar.setTimeZone(TimeZone.getTimeZone(timezones[0]));
        }
        return Optional.of(calendar.getTime());
    }

    /**
     * Convert a date object describing the volume creation data according to ECMA-119 8.4.26.1. These are used in ISO
     * Primary volume descriptors.
     *
     * @param dateTime bytes of the date and time to convert
     * @return java formatted Date as an optional
     * @throws ParseException If the bytes cannot be converted, a ParseException can be thrown.
     */
    public static Optional<Date> convert8_4_26_1DateTime(byte[] dateTime) throws ParseException {
        String dateAsString = new String(Arrays.copyOf(dateTime, 16), StandardCharsets.UTF_8);
        // 2019112123095100
        if (dateAsString.length() != 16) {
            return Optional.empty();
        }
        DateFormat df = new SimpleDateFormat("yyyyMMddkkmmssSS", Locale.ENGLISH);
        int offset = dateTime[16]; // This is a signed number, GMT offset 15 minutes * this
        int rawOffsetAdjustedForJava = offset * 15 * 60 * 1000;
        String[] timezones = TimeZone.getAvailableIDs(rawOffsetAdjustedForJava);
        if (timezones.length > 0) { // If an offset is entered and a timezone at that location doesnt exist
            df.setTimeZone(TimeZone.getTimeZone(timezones[0]));
        }
        return Optional.ofNullable(df.parse(dateAsString));
    }

    /**
     * Convert UDF style dStrings into Java Strings.
     *
     * @param inputData byte array of dString
     * @return Java String
     */
    public static String convertDStringBytesToString(byte[] inputData) {
        if (inputData.length == 0) {
            return "";
        }
        int compressionId = Byte.toUnsignedInt(inputData[0]);
        Charset usingCharset = StandardCharsets.UTF_8;
        if ((compressionId == 16 || compressionId == 255)) {
            usingCharset = StandardCharsets.UTF_16;
        }
        // I have yet to find this in use, but it could be in the future.
        // TODO(#57): Implement dString compression.
        // boolean isCompressed = (compressionId == 254 || compressionId == 255);

        int realLength = inputData.length;
        // For some reason some fields seem to have an extra bit at the end.
        for (int i = inputData.length - 1; i >= 0; i--) {
            if (inputData[i] != 0x00) {
                realLength = i;
                break;
            }
        }

        return new String(Arrays.copyOfRange(inputData, 1, realLength + 1), usingCharset);
    }

    /**
     * Convert to unsigned byte array into a Java Int.
     *
     * @param data unsigned array
     * @return java int
     */
    public static int twoUnsignedByteToInt(byte[] data) {
        byte[] temp = new byte[4];
        temp[1] = data[1];
        temp[0] = data[0];
        final ByteBuffer bb = ByteBuffer.wrap(temp);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    /**
     * Convert from four unsigned bytes to a java int.
     *
     * @param data 4 byte array
     * @return java int
     */
    public static int fourUnsignedByteToInt(byte[] data) {
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }
}

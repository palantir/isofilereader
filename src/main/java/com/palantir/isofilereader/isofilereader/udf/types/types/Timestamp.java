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

import com.palantir.isofilereader.isofilereader.Util;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * UDF Style, UDF 2.60 3.1.1.
 */
public class Timestamp {
    // Uint16 TypeAndTimezone;
    private final byte[] typeAndTimezone;
    // Int16 Year;
    private final byte[] year;
    // Uint8 Month;
    private final byte month;
    // Uint8 Day;
    private final byte day;
    // Uint8 Hour;
    private final byte hour;
    // Uint8 Minute;
    private final byte minute;
    // Uint8 Second;
    private final byte second;
    // Uint8 Centiseconds;
    private final byte centiseconds;
    // Uint8 HundredsofMicroseconds;
    private final byte hundredsOfMicroseconds;
    // Uint8 Microseconds;
    private final byte microseconds;

    /**
     * Timestamp according to UDF 2.60 2.1.4.
     *
     * @param record 12 bytes of data
     */
    public Timestamp(byte[] record) {
        this.typeAndTimezone = Arrays.copyOfRange(record, 0, 2);
        this.year = Arrays.copyOfRange(record, 2, 4);
        this.month = record[4];
        this.day = record[5];
        this.hour = record[6];
        this.minute = record[7];
        this.second = record[8];
        this.centiseconds = record[9];
        this.hundredsOfMicroseconds = record[10];
        this.microseconds = record[11];
    }

    /**
     * 2 bytes of type of timestamp and time zone.
     *
     * @return 2 bytes
     */
    public byte[] getTypeAndTimezone() {
        return typeAndTimezone;
    }

    /**
     * Get the most significant 4 bits of the first field. This is supposed to be 1 to indicate "local time".
     *
     * @return 1 for local time
     */
    public int getTypeOfTimestamp() {
        return ((int) (Util.twoUnsignedByteToInt(getTypeAndTimezone()) & Long.parseUnsignedLong("1111000000000000")))
                >> 12; // "1111000000000000"
    }

    /**
     * The lower 12 significant bits are the timezone by offset, 1 = 1 minute away from GMT, is signed.
     * This field is in twos complement for some reason. <a href="https://www.exploringbinary.com/twos-complement-converter/">this site</a>
     * is a good example.
     *
     * @return int of time offset in minutes
     */
    @SuppressWarnings("StrictUnusedVariable")
    public int getMinuteOffsetFromGmt() {
        byte byteOne = (byte) (Byte.toUnsignedInt(getTypeAndTimezone()[1]) & Integer.parseUnsignedInt("00001111", 2));
        byte byteTwo = getTypeAndTimezone()[0];
        byte isNegativeBit =
                (byte) (Byte.toUnsignedInt(getTypeAndTimezone()[1]) & Integer.parseUnsignedInt("00001000", 2));
        boolean isNegativeNumber = (isNegativeBit >> 3) != 0;

        int signedBits = (byteOne << 8 | (byteTwo & 0xff));
        if (isNegativeNumber) {
            signedBits = signedBits - 4096;
        }
        return signedBits;
    }

    /**
     * Year in two bytes.
     *
     * @return 2 bytes of year
     */
    public byte[] getYear() {
        return year;
    }

    /**
     * The month as a byte.
     *
     * @return byte of month
     */
    public byte getMonth() {
        return month;
    }

    /**
     * Get the day of the month.
     *
     * @return byte of day
     */
    public byte getDay() {
        return day;
    }

    /**
     * Get the hour of the day in 24 hour time.
     *
     * @return byte of hour
     */
    public byte getHour() {
        return hour;
    }

    /**
     * Get the minute of the hour.
     *
     * @return byte of minute
     */
    public byte getMinute() {
        return minute;
    }

    /**
     * Get Second. 1 second.
     *
     * @return byte of second
     */
    public byte getSecond() {
        return second;
    }

    /**
     * Get centisecond, 100 centiseconds are 1 second. 0.01 seconds.
     *
     * @return byte of centisecond
     */
    public byte getCentiseconds() {
        return centiseconds;
    }

    /**
     * Hundredths of a microsecond. 0.0000001 seconds.
     *
     * @return byte of microseconds
     */
    public byte getHundredsOfMicroseconds() {
        return hundredsOfMicroseconds;
    }

    /**
     * Microseconds. 0.000001 seconds
     *
     * @return byte
     */
    public byte getMicroseconds() {
        return microseconds;
    }

    /**
     * Get the last timestamp as a Java Date.
     *
     * @return Date
     */
    public Date getAsDate() {
        Calendar calendar = new GregorianCalendar(
                Util.twoUnsignedByteToInt(getYear()), getMonth() - 1, getDay(), getHour(), getMinute(), getSecond());

        int rawOffsetAdjustedForJava = getMinuteOffsetFromGmt() * 60 * 1000;
        // Need to convert to milliseconds
        String[] timezones = TimeZone.getAvailableIDs(rawOffsetAdjustedForJava);
        if (timezones.length > 0) { // If an offset is entered and a timezone at that location doesn't exist
            calendar.setTimeZone(TimeZone.getTimeZone(timezones[0]));
        }
        return calendar.getTime();
    }
}

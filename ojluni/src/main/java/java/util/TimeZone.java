/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (c) 1996, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import android.icu.text.TimeZoneNames;
import com.android.i18n.timezone.ZoneInfoData;
import com.android.i18n.timezone.ZoneInfoDb;
import com.android.icu.util.ExtendedTimeZone;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.util.ZoneInfo;
import java.time.ZoneOffset;

import dalvik.system.RuntimeHooks;

/**
 * {@code TimeZone} represents a time zone offset, and also figures out daylight
 * savings.
 *
 * <p>
 * Typically, you get a {@code TimeZone} using {@code getDefault}
 * which creates a {@code TimeZone} based on the time zone where the program
 * is running. For example, for a program running in Japan, {@code getDefault}
 * creates a {@code TimeZone} object based on Japanese Standard Time.
 *
 * <p>
 * You can also get a {@code TimeZone} using {@code getTimeZone}
 * along with a time zone ID. For instance, the time zone ID for the
 * U.S. Pacific Time zone is "America/Los_Angeles". So, you can get a
 * U.S. Pacific Time {@code TimeZone} object with:
 * <blockquote>
 * {@snippet lang=java :
 * TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
 * }
 * </blockquote>
 * You can use the {@code getAvailableIDs} method to iterate through
 * all the supported time zone IDs. You can then choose a
 * supported ID to get a {@code TimeZone}.
 * If the time zone you want is not represented by one of the
 * supported IDs, then a custom time zone ID can be specified to
 * produce a TimeZone. The syntax of a custom time zone ID is:
 *
 * <blockquote><pre>
 * <a id="CustomID"><i>CustomID:</i></a>
 *         {@code GMT} <i>Sign</i> <i>Hours</i> {@code :} <i>Minutes</i> {@code :} <i>Seconds</i>
 *         {@code GMT} <i>Sign</i> <i>Hours</i> {@code :} <i>Minutes</i>
 *         {@code GMT} <i>Sign</i> <i>Hours</i> <i>Minutes</i>
 *         {@code GMT} <i>Sign</i> <i>Hours</i>
 * <i>Sign:</i> one of
 *         {@code + -}
 * <i>Hours:</i>
 *         <i>Digit</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Seconds:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         {@code 0 1 2 3 4 5 6 7 8 9}
 * </pre></blockquote>
 *
 * <i>Hours</i> must be between 0 to 23 and <i>Minutes</i>/<i>Seconds</i> must be
 * between 00 to 59.  For example, "GMT+10" and "GMT+0010" mean ten
 * hours and ten minutes ahead of GMT, respectively.
 * <p>
 * The format is locale independent and digits must be taken from the
 * Basic Latin block of the Unicode standard. No daylight saving time
 * transition schedule can be specified with a custom time zone ID. If
 * the specified string doesn't match the syntax, {@code "GMT"}
 * is used.
 * <p>
 * When creating a {@code TimeZone}, the specified custom time
 * zone ID is normalized in the following syntax:
 * <blockquote><pre>
 * <a id="NormalizedCustomID"><i>NormalizedCustomID:</i></a>
 *         {@code GMT} <i>Sign</i> <i>TwoDigitHours</i> {@code :} <i>Minutes</i> [<i>ColonSeconds</i>]
 * <i>Sign:</i> one of
 *         {@code + -}
 * <i>TwoDigitHours:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>ColonSeconds:</i>
 *         {@code :} <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         {@code 0 1 2 3 4 5 6 7 8 9}
 * </pre></blockquote>
 * For example, TimeZone.getTimeZone("GMT-8").getID() returns "GMT-08:00".
 * <i>ColonSeconds</i> part only appears if the seconds value is non-zero.
 *
 * <h2>Three-letter time zone IDs</h2>
 *
 * For compatibility with JDK 1.1.x, some other three-letter time zone IDs
 * (such as "PST", "CTT", "AST") are also supported. However, <strong>their
 * use is deprecated</strong> because the same abbreviation is often used
 * for multiple time zones (for example, "CST" could be U.S. "Central Standard
 * Time" and "China Standard Time"), and the Java platform can then only
 * recognize one of them.
 *
 *
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author       Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @since        1.1
 */
public abstract class TimeZone implements Serializable, Cloneable {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    public TimeZone() {
    }

    /**
     * A style specifier for {@code getDisplayName()} indicating
     * a short name, such as "PST."
     * @see #LONG
     * @since 1.2
     */
    public static final int SHORT = 0;

    /**
     * A style specifier for {@code getDisplayName()} indicating
     * a long name, such as "Pacific Standard Time."
     * @see #SHORT
     * @since 1.2
     */
    public static final int LONG  = 1;

    // Android-changed: Use a preload holder to allow compile-time initialization of TimeZone and
    // dependents.
    private static class NoImagePreloadHolder {
        // Custom time zone ID can only have one of the following forms:
        // 1. GMT Sign Hours
        // 2. GMT Sign Hours Minutes
        // 3. GMT Sign Hours : Minutes
        // 4. GMT Sign Hours : Minutes : Seconds
        // The second capturing group is either:
        // * Absent (then input matches the 1st form).
        // * Consists of only 2 digits (matches the 2nd form).
        // * Consists of colon and 2 digits (matches the 3rd form).
        // * Or has the 4th form: colon followed by 2 digits, followed by colon and 2 digits.
        public static final Pattern CUSTOM_ZONE_ID_PATTERN =
                Pattern.compile("^GMT[-+](\\d{1,2})((\\d\\d)|:((\\d\\d)(:(\\d\\d))?))?");
    }

    // Proclaim serialization compatibility with JDK 1.1
    @java.io.Serial
    static final long serialVersionUID = 3581463369166924961L;

    // Android-changed: common timezone instances.
    private static final TimeZone GMT = new SimpleTimeZone(0, "GMT");
    private static final TimeZone UTC = new SimpleTimeZone(0, "UTC");

    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add to UTC to get local time.
     * <p>
     * This method returns a historically correct offset if an
     * underlying {@code TimeZone} implementation subclass
     * supports historical Daylight Saving Time schedule and GMT
     * offset changes.
     *
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the milliseconds in day in <em>standard</em>
     * local time.
     *
     * @return the offset in milliseconds to add to GMT to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    public abstract int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int milliseconds);

    /**
     * Returns the offset of this time zone from UTC at the specified
     * date. If Daylight Saving Time is in effect at the specified
     * date, the offset value is adjusted with the amount of daylight
     * saving.
     * <p>
     * This method returns a historically correct offset value if an
     * underlying TimeZone implementation subclass supports historical
     * Daylight Saving Time schedule and GMT offset changes.
     *
     * @param date the date represented in milliseconds since January 1, 1970 00:00:00 GMT
     * @return the amount of time in milliseconds to add to UTC to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     * @since 1.4
     */
    public int getOffset(long date) {
        if (inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    /**
     * Gets the raw GMT offset and the amount of daylight saving of this
     * time zone at the given time.
     * @param date the milliseconds (since January 1, 1970,
     * 00:00:00.000 GMT) at which the time zone offset and daylight
     * saving amount are found
     * @param offsets an array of int where the raw GMT offset
     * (offset[0]) and daylight saving amount (offset[1]) are stored,
     * or null if those values are not needed. The method assumes that
     * the length of the given array is two or larger.
     * @return the total amount of the raw GMT offset and daylight
     * saving at the specified date.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    int getOffsets(long date, int[] offsets) {
        int rawoffset = getRawOffset();
        int dstoffset = 0;
        if (inDaylightTime(new Date(date))) {
            dstoffset = getDSTSavings();
        }
        if (offsets != null) {
            offsets[0] = rawoffset;
            offsets[1] = dstoffset;
        }
        return rawoffset + dstoffset;
    }

    /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add to UTC to get local time.
     * <p>
     * If an underlying {@code TimeZone} implementation subclass
     * supports historical GMT offset changes, the specified GMT
     * offset is set as the latest GMT offset and the difference from
     * the known latest GMT offset value is used to adjust all
     * historical GMT offset values.
     *
     * @param offsetMillis the given base time zone offset to GMT.
     */
    public abstract void setRawOffset(int offsetMillis);

    /**
     * Returns the amount of time in milliseconds to add to UTC to get
     * standard time in this time zone. Because this value is not
     * affected by daylight saving time, it is called <I>raw
     * offset</I>.
     * <p>
     * If an underlying {@code TimeZone} implementation subclass
     * supports historical GMT offset changes, the method returns the
     * raw offset value of the current date. In Honolulu, for example,
     * its raw offset changed from GMT-10:30 to GMT-10:00 in 1947, and
     * this method always returns -36000000 milliseconds (i.e., -10
     * hours).
     *
     * @return the amount of raw offset time in milliseconds to add to UTC.
     * @see Calendar#ZONE_OFFSET
     */
    public abstract int getRawOffset();

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     */
    public String getID()
    {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @implSpec The default implementation throws a
     * {@code NullPointerException} if {@code ID} is {@code null}
     * @param ID the new time zone ID.
     * @throws NullPointerException This method may throw a
     * {@code NullPointerException} if {@code ID} is {@code null}
     */
    public void setID(String ID)
    {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
    }

    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the default locale.
     *
     * <p>This method is equivalent to:
     * <blockquote>
     * {@snippet lang=java :
     * // @link substring="LONG" target="#LONG" :
     * getDisplayName(false, LONG,
     *                // @link substring="Locale.Category.DISPLAY" target="Locale.Category#DISPLAY" :
     *                Locale.getDefault(Locale.Category.DISPLAY));
     * }
     * </blockquote>
     *
     * @return the human-readable name of this time zone in the default locale.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     */
    public final String getDisplayName() {
        return getDisplayName(false, LONG,
                              Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the specified {@code locale}.
     *
     * <p>This method is equivalent to:
     * <blockquote>
     * {@snippet lang=java :
     * // @link substring="LONG" target="#LONG" :
     * getDisplayName(false, LONG, locale);
     * }
     * </blockquote>
     *
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale.
     * @throws    NullPointerException if {@code locale} is {@code null}.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     */
    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }

    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the default locale. If the
     * specified {@code daylight} is {@code true}, a Daylight Saving Time name
     * is returned (even if this {@code TimeZone} doesn't observe Daylight Saving
     * Time). Otherwise, a Standard Time name is returned.
     *
     * <p>This method is equivalent to:
     * <blockquote>
     * {@snippet lang=java :
     * getDisplayName(daylight, style,
     *                // @link substring="Locale.Category.DISPLAY" target="Locale.Category#DISPLAY" :
     *                Locale.getDefault(Locale.Category.DISPLAY));
     * }
     * </blockquote>
     *
     * @param daylight {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style either {@link #LONG} or {@link #SHORT}
     * @return the human-readable name of this time zone in the default locale.
     * @throws    IllegalArgumentException if {@code style} is invalid.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     * @see java.text.DateFormatSymbols#getZoneStrings()
     */
    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style,
                              Locale.getDefault(Locale.Category.DISPLAY));
    }

    // Android-changed: ResourceBundle section removed.
    /**
     * Returns the {@link #SHORT short} or {@link #LONG long} name of this time
     * zone with either standard or daylight time, as written in {@code locale}.
     * If the name is not available, the result is in the format
     * {@code GMT[+-]hh:mm}.
     *
     * @implSpec The default implementation throws an
     * {@code IllegalArgumentException} if {@code style} is invalid or a
     * {@code NullPointerException} if {@code ID} is {@code null}.
     * @param daylightTime {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style either {@link #LONG} or {@link #SHORT}
     * @param locale   the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale.
     * @throws IllegalArgumentException This method may throw an
     * {@code IllegalArgumentException} if {@code style} is invalid.
     * @throws NullPointerException This method may throw a
     * {@code NullPointerException} if {@code ID} is {@code null}
     * @since 1.2
     * @see java.text.DateFormatSymbols#getZoneStrings()
     */
    // Android-changed: daylight -> daylightTime.
    public String getDisplayName(boolean daylightTime, int style, Locale locale) {
        // BEGIN Android-changed: implement using android.icu.text.TimeZoneNames
        TimeZoneNames.NameType nameType;
        switch (style) {
            case SHORT:
                nameType = daylightTime
                        ? TimeZoneNames.NameType.SHORT_DAYLIGHT
                        : TimeZoneNames.NameType.SHORT_STANDARD;
                break;
            case LONG:
                nameType = daylightTime
                        ? TimeZoneNames.NameType.LONG_DAYLIGHT
                        : TimeZoneNames.NameType.LONG_STANDARD;
                break;
            default:
                throw new IllegalArgumentException("Illegal style: " + style);
        }
        String canonicalID = android.icu.util.TimeZone.getCanonicalID(getID());
        if (canonicalID != null) {
            TimeZoneNames names = TimeZoneNames.getInstance(locale);
            long now = System.currentTimeMillis();
            String displayName = names.getDisplayName(canonicalID, nameType, now);
            if (displayName != null) {
                return displayName;
            }
        }

        // We get here if this is a custom timezone or ICU doesn't have name data for the specific
        // style and locale.
        int offsetMillis = getRawOffset();
        if (daylightTime) {
            offsetMillis += getDSTSavings();
        }
        return createGmtOffsetString(true /* includeGmt */, true /* includeMinuteSeparator */,
                offsetMillis);
        // END Android-changed: implement using android.icu.text.TimeZoneNames
    }

    // BEGIN Android-added: utility method to format an offset as a GMT offset string.
    /**
     * Returns a string representation of an offset from UTC.
     *
     * <p>The format is "[GMT](+|-)HH[:]MM". The output is not localized.
     *
     * @param includeGmt true to include "GMT", false to exclude
     * @param includeMinuteSeparator true to include the separator between hours and minutes, false
     *     to exclude.
     * @param offsetMillis the offset from UTC
     *
     * @hide used internally by SimpleDateFormat
     */
    public static String createGmtOffsetString(boolean includeGmt,
            boolean includeMinuteSeparator, int offsetMillis) {
        int offsetMinutes = offsetMillis / 60000;
        char sign = '+';
        if (offsetMinutes < 0) {
            sign = '-';
            offsetMinutes = -offsetMinutes;
        }
        StringBuilder builder = new StringBuilder(9);
        if (includeGmt) {
            builder.append("GMT");
        }
        builder.append(sign);
        appendNumber(builder, 2, offsetMinutes / 60);
        if (includeMinuteSeparator) {
            builder.append(':');
        }
        appendNumber(builder, 2, offsetMinutes % 60);
        return builder.toString();
    }

    private static void appendNumber(StringBuilder builder, int count, int value) {
        String string = Integer.toString(value);
        for (int i = 0; i < count - string.length(); i++) {
            builder.append('0');
        }
        builder.append(string);
    }
    // END Android-added: utility method to format an offset as a GMT offset string.

    /**
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     *
     * <p>The default implementation returns 3600000 milliseconds
     * (i.e., one hour) if a call to {@link #useDaylightTime()}
     * returns {@code true}. Otherwise, 0 (zero) is returned.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method returns the amount of saving time of the
     * last known Daylight Saving Time rule that can be a future
     * prediction.
     *
     * <p>If the amount of saving time at any given time stamp is
     * required, construct a {@link Calendar} with this {@code
     * TimeZone} and the time stamp, and call {@link Calendar#get(int)
     * Calendar.get}{@code (}{@link Calendar#DST_OFFSET}{@code )}.
     *
     * @return the amount of saving time in milliseconds
     * @since 1.4
     * @see #inDaylightTime(Date)
     * @see #getOffset(long)
     * @see #getOffset(int,int,int,int,int,int)
     * @see Calendar#ZONE_OFFSET
     */
    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    /**
     * Queries if this {@code TimeZone} uses Daylight Saving Time.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method refers to the last known Daylight Saving Time
     * rule that can be a future prediction and may not be the same as
     * the current rule. Consider calling {@link #observesDaylightTime()}
     * if the current rule should also be taken into account.
     *
     * @return {@code true} if this {@code TimeZone} uses Daylight Saving Time,
     *         {@code false}, otherwise.
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     */
    public abstract boolean useDaylightTime();

    /**
     * Returns {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time.
     *
     * <p>The default implementation returns {@code true} if
     * {@code useDaylightTime()} or {@code inDaylightTime(new Date())}
     * returns {@code true}.
     *
     * @return {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time; {@code false}
     * otherwise.
     * @since 1.7
     * @see #useDaylightTime()
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     */
    public boolean observesDaylightTime() {
        return useDaylightTime() || inDaylightTime(new Date());
    }

    /**
     * Queries if the given {@code date} is in Daylight Saving Time in
     * this time zone.
     *
     * @param date the given Date.
     * @return {@code true} if the given date is in Daylight Saving Time,
     *         {@code false}, otherwise.
     * @throws NullPointerException This method may throw a
     * {@code NullPointerException} if {@code date} is {@code null}
     */
    public abstract boolean inDaylightTime(Date date);

    /**
     * Gets the {@code TimeZone} for the given ID.
     *
     * @param id the ID for a <code>TimeZone</code>, either an abbreviation
     * such as "PST", a full name such as "America/Los_Angeles", or a custom
     * ID such as "GMT-8:00". Note that the support of abbreviations is
     * for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified {@code TimeZone}, or the GMT zone if the given ID
     * cannot be understood.
     * @throws NullPointerException if {@code ID} is {@code null}
     */
    // Android-changed: param s/ID/id; use ZoneInfoDb instead of ZoneInfo class.
    public static synchronized TimeZone getTimeZone(String id) {
        if (id == null) {
            throw new NullPointerException("id == null");
        }

        // Special cases? These can clone an existing instance.
        if (id.length() == 3) {
            if (id.equals("GMT")) {
                return (TimeZone) GMT.clone();
            }
            if (id.equals("UTC")) {
                return (TimeZone) UTC.clone();
            }
        }

        // In the database?

        ZoneInfoData zoneInfoData = ZoneInfoDb.getInstance().makeZoneInfoData(id);
        TimeZone zone = zoneInfoData == null ? null : ZoneInfo.createZoneInfo(zoneInfoData);

        // Custom time zone?
        if (zone == null && id.length() > 3 && id.startsWith("GMT")) {
            zone = getCustomTimeZone(id);
        }

        // We never return null; on failure we return the equivalent of "GMT".
        return (zone != null) ? zone : (TimeZone) GMT.clone();
    }

    /**
     * Gets the {@code TimeZone} for the given {@code zoneId}.
     *
     * @param zoneId a {@link ZoneId} from which the time zone ID is obtained
     * @return the specified {@code TimeZone}, or the GMT zone if the given ID
     *         cannot be understood.
     * @throws NullPointerException if {@code zoneId} is {@code null}
     * @since 1.8
     */
    public static TimeZone getTimeZone(ZoneId zoneId) {
        String tzid = zoneId.getId(); // throws an NPE if null
        // BEGIN Android-removed: sun.util.calendar.ZoneInfo is not available.
        /*
        if (zoneId instanceof ZoneOffset zo) {
            var totalMillis = zo.getTotalSeconds() * 1_000;
            return new ZoneInfo(totalMillis == 0 ? "UTC" : GMT_ID + tzid, totalMillis);
        } else if (tzid.startsWith("UT") && !tzid.equals("UTC")) {
            tzid = tzid.replaceFirst("(UTC|UT)(.*)", "GMT$2");
        }
        */
        // END Android-removed: sun.util.calendar.ZoneInfo is not available.
        char c = tzid.charAt(0);
        if (c == '+' || c == '-') {
            tzid = "GMT" + tzid;
        } else if (c == 'Z' && tzid.length() == 1) {
            tzid = "UTC";
        }
        return getTimeZone(tzid);
    }

    /**
     * Converts this {@code TimeZone} object to a {@code ZoneId}.
     *
     * @return a {@code ZoneId} representing the same time zone as this
     *         {@code TimeZone}
     * @since 1.8
     */
    public ZoneId toZoneId() {
        // Android-changed: don't support "old mapping"
        return ZoneId.of(getID(), ZoneId.SHORT_IDS);
    }

    /**
     * Returns a new SimpleTimeZone for an ID of the form either "GMT[+|-]hh[[:]mm]" or
     * GMT[+|-]hh:mm:SS, or null.
     */
    private static TimeZone getCustomTimeZone(String id) {
        Matcher m = NoImagePreloadHolder.CUSTOM_ZONE_ID_PATTERN.matcher(id);
        if (!m.matches()) {
            return null;
        }

        int hour;
        int minute = 0;
        int second = 0;
        try {
            hour = Integer.parseInt(m.group(1));
            if (m.group(3) != null) {
                minute = Integer.parseInt(m.group(3));
            }
            if (m.group(5) != null) {
                minute = Integer.parseInt(m.group(5));
            }
            if (m.group(7) != null) {
                second = Integer.parseInt(m.group(7));
            }
        } catch (NumberFormatException impossible) {
            throw new AssertionError(impossible);
        }

        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
            return null;
        }

        char sign = id.charAt(3);
        int raw = (hour * 3600000) + (minute * 60000) + second * 1000;
        if (sign == '-') {
            raw = -raw;
        }

        String cleanId;
        if (second == 0) {
            cleanId = String.format(Locale.ROOT, "GMT%c%02d:%02d", sign, hour, minute);
        } else {
            cleanId = String.format(Locale.ROOT, "GMT%c%02d:%02d:%02d", sign, hour, minute, second);
        }

        return new SimpleTimeZone(raw, cleanId);
    }

    /**
     * Gets the available IDs according to the given time zone offset in milliseconds.
     *
     * @param rawOffset the given time zone GMT offset in milliseconds.
     * @return an array of IDs, where the time zone for that ID has
     * the specified GMT offset. For example, "America/Phoenix" and "America/Denver"
     * both have GMT-07:00, but differ in daylight saving behavior.
     * @see #getRawOffset()
     */
    public static synchronized String[] getAvailableIDs(int rawOffset) {
        return ZoneInfoDb.getInstance().getAvailableIDs(rawOffset);
    }

    /**
     * Gets all the available IDs supported.
     * @return an array of IDs.
     */
    public static synchronized String[] getAvailableIDs() {
        return ZoneInfoDb.getInstance().getAvailableIDs();
    }

    /**
     * Gets the platform defined TimeZone ID.
     **/
    private static native String getSystemTimeZoneID(String javaHome,
                                                     String country);

    /**
     * Gets the custom time zone ID based on the GMT offset of the
     * platform. (e.g., "GMT+08:00")
     */
    private static native String getSystemGMTOffsetID();

    // Android-changed: removed section about the way default time zone ID is stored / fetched.
    /**
     * Gets the default <code>TimeZone</code> for this host.
     * The source of the default <code>TimeZone</code>
     * may vary with implementation.
     * @return a default <code>TimeZone</code>.
     * @see #setDefault(TimeZone)
     */
    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }

    /**
     * Returns the reference to the default TimeZone object. This
     * method doesn't create a clone.
     */
    static synchronized TimeZone getDefaultRef() {
        if (defaultTimeZone == null) {
            Supplier<String> tzGetter = RuntimeHooks.getTimeZoneIdSupplier();
            String zoneName = (tzGetter != null) ? tzGetter.get() : null;
            if (zoneName != null) {
                zoneName = zoneName.trim();
            }
            if (zoneName == null || zoneName.isEmpty()) {
                try {
                    // On the host, we can find the configured timezone here.
                    zoneName = IoUtils.readFileAsString("/etc/timezone");
                } catch (IOException ex) {
                    // "vogar --mode device" can end up here.
                    // TODO: give libcore access to Android system properties and read "persist.sys.timezone".
                    zoneName = GMT_ID;
                }
            }
            defaultTimeZone = TimeZone.getTimeZone(zoneName);
        }
        return defaultTimeZone;
    }

    // BEGIN Android-removed: different getDefault / setDefault implementation.
    /*
     * Returns the reference to the default TimeZone object. This
     * method doesn't create a clone.
     *
    static TimeZone getDefaultRef() {
        TimeZone defaultZone = defaultTimeZone;
        if (defaultZone == null) {
            // Need to initialize the default time zone.
            defaultZone = setDefaultZone();
            assert defaultZone != null;
        }
        // Don't clone here.
        return defaultZone;
    }

    private static synchronized TimeZone setDefaultZone() {
        TimeZone tz;
        // get the time zone ID from the system properties
        Properties props = GetPropertyAction.privilegedGetProperties();
        String zoneID = props.getProperty("user.timezone");

        // if the time zone ID is not set (yet), perform the
        // platform to Java time zone ID mapping.
        if (zoneID == null || zoneID.isEmpty()) {
            zoneID = getSystemTimeZoneID(StaticProperty.javaHome());
            if (zoneID == null) {
                zoneID = GMT_ID;
            }
        }

        // Get the time zone for zoneID. But not fall back to
        // "GMT" here.
        tz = getTimeZone(zoneID, false);

        if (tz == null) {
            // If the given zone ID is unknown in Java, try to
            // get the GMT-offset-based time zone ID,
            // a.k.a. custom time zone ID (e.g., "GMT-08:00").
            String gmtOffsetID = getSystemGMTOffsetID();
            if (gmtOffsetID != null) {
                zoneID = gmtOffsetID;
            }
            tz = getTimeZone(zoneID, true);
        }
        assert tz != null;

        final String id = zoneID;
        props.setProperty("user.timezone", id);

        defaultTimeZone = tz;
        return tz;
    }
    */
    // END Android-removed: different getDefault / setDefault implementation.

    /**
     * Sets the {@code TimeZone} that is returned by the {@code getDefault}
     * method. {@code zone} is cached. If {@code zone} is null, the cached
     * default {@code TimeZone} is cleared. This method doesn't change the value
     * of the {@code user.timezone} property.
     *
     * @param timeZone the new default {@code TimeZone}, or null
     * @see #getDefault
     */
    // Android-changed: s/zone/timeZone, synchronized, removed mention of SecurityException
    public synchronized static void setDefault(TimeZone timeZone)
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new PropertyPermission
                    ("user.timezone", "write"));
        }
        // by saving a defensive clone and returning a clone in getDefault() too,
        // the defaultTimeZone instance is isolated from user code which makes it
        // effectively immutable. This is important to avoid races when the
        // following is evaluated in ZoneId.systemDefault():
        // TimeZone.getDefault().toZoneId().
        defaultTimeZone = (timeZone == null) ? null : (TimeZone) timeZone.clone();
        // Android-changed: notify ICU4J of changed default TimeZone.
        ExtendedTimeZone.clearDefaultTimeZone();
    }

    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.  Returns false
     * if the other zone is null.
     * @param other the {@code TimeZone} object to be compared with
     * @return true if the other zone is not null and is the same as this one,
     * with the possible exception of the ID
     * @since 1.2
     */
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Creates a copy of this {@code TimeZone}.
     *
     * @return a clone of this {@code TimeZone}
     */
    public Object clone()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * The null constant as a TimeZone.
     */
    static final TimeZone NO_TIMEZONE = null;

    // =======================privates===============================

    /**
     * The string identifier of this {@code TimeZone}.  This is a
     * programmatic identifier used internally to look up {@code TimeZone}
     * objects from the system table and also to map them to their localized
     * display names.  {@code ID} values are unique in the system
     * table but may not be for dynamically created zones.
     * @serial
     */
    private String           ID;
    private static volatile TimeZone defaultTimeZone;

    static final String         GMT_ID        = "GMT";
    // Android-removed: different setDefault / getDefault implementation.
    /*
    private static final int    GMT_ID_LENGTH = 3;

     *
     * Parses a custom time zone identifier and returns a corresponding zone.
     * This method doesn't support the RFC 822 time zone format. (e.g., +hhmm)
     *
     * @param id a string of the <a href="#CustomID">custom ID form</a>.
     * @return a newly created TimeZone with the given offset and
     * no daylight saving time, or null if the id cannot be parsed.
     *
    private static final TimeZone parseCustomTimeZone(String id) {
        int length;

        // Error if the length of id isn't long enough or id doesn't
        // start with "GMT".
        if ((length = id.length()) < (GMT_ID_LENGTH + 2) ||
            id.indexOf(GMT_ID) != 0) {
            return null;
        }

        ZoneInfo zi;

        // First, we try to find it in the cache with the given
        // id. Even the id is not normalized, the returned ZoneInfo
        // should have its normalized id.
        zi = ZoneInfoFile.getZoneInfo(id);
        if (zi != null) {
            return zi;
        }

        int index = GMT_ID_LENGTH;
        boolean negative = false;
        char c = id.charAt(index++);
        if (c == '-') {
            negative = true;
        } else if (c != '+') {
            return null;
        }

        int hours = 0;
        int minutes = 0;
        int num = 0;
        int countDelim = 0;
        int len = 0;
        while (index < length) {
            c = id.charAt(index++);
            if (c == ':') {
                if (countDelim > 1) {
                    return null;
                }
                if (len == 0 || len > 2) {
                    return null;
                }
                if (countDelim == 0) {
                    hours = num;
                } else if (countDelim == 1){
                    minutes = num;
                }
                countDelim++;
                num = 0;
                len = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
            num = num * 10 + (c - '0');
            len++;
        }
        if (index != length) {
            return null;
        }
        if (countDelim == 0) {
            if (len <= 2) {
                hours = num;
                minutes = 0;
                num = 0;
            } else if (len <= 4) {
                hours = num / 100;
                minutes = num % 100;
                num = 0;
            } else {
                return null;
            }
        } else if (countDelim == 1){
            if (len == 2) {
                minutes = num;
                num = 0;
            } else {
                return null;
            }
        } else {
            if (len != 2) {
                return null;
            }
        }
        if (hours > 23 || minutes > 59 || num > 59) {
            return null;
        }
        int gmtOffset =  (hours * 3_600 + minutes * 60 + num) * 1_000;

        if (gmtOffset == 0) {
            zi = ZoneInfoFile.getZoneInfo(GMT_ID);
            if (negative) {
                zi.setID("GMT-00:00");
            } else {
                zi.setID("GMT+00:00");
            }
        } else {
            zi = ZoneInfoFile.getCustomTimeZone(id, negative ? -gmtOffset : gmtOffset);
        }
        return zi;
    }
    */
}

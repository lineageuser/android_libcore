/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import static java.nio.charset.StandardCharsets.UTF_8;

import dalvik.annotation.compat.VersionCodes;
import dalvik.system.VMRuntime;

import libcore.junit.util.compat.CoreCompatChangeRule;
import libcore.junit.util.compat.CoreCompatChangeRule.DisableCompatChanges;
import libcore.junit.util.compat.CoreCompatChangeRule.EnableCompatChanges;
import libcore.test.annotation.NonMts;
import libcore.test.reasons.NonMtsReasons;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.FormatFlagsConversionMismatchException;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.TimeZone;

@SuppressWarnings("FormatString")
public class FormatterTest {

    @Rule
    public final TestRule compatChangeRule = new CoreCompatChangeRule();

    private File aFile;

    @Before
    public void setUp() throws Exception {
        aFile = File.createTempFile("libcore_java_util_FormatterTest-test-file", null);
        aFile.deleteOnExit();
    }

    @After
    public void cleanup() throws Exception {
        aFile.delete();
    }

    @NonMts(reason = NonMtsReasons.ICU_VERSION_DEPENDENCY,
        disabledUntilSdk = VersionCodes.VANILLA_ICE_CREAM)
    @Test
    public void test_numberLocalization() throws Exception {
        Locale arabic = new Locale("ar");

        // Check the fast path for %d:
        assertEquals("12 \u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669\u0660 34",
                String.format(arabic, "12 %d 34", 1234567890));
        // And the slow path too:
        assertEquals("12 \u0661\u066c\u0662\u0663\u0664\u066c\u0665\u0666\u0667\u066c\u0668\u0669\u0660 34",
                String.format(arabic, "12 %,d 34", 1234567890));
        // And three localized floating point formats:
        assertEquals("12 \u0661\u066b\u0662\u0663\u0660\u0623\u0633+\u0660\u0660 34",
                String.format(arabic, "12 %.3e 34", 1.23));
        assertEquals("12 \u0661\u066b\u0662\u0663\u0660 34",
                String.format(arabic, "12 %.3f 34", 1.23));
        assertEquals("12 \u0661\u066b\u0662\u0663 34",
                String.format(arabic, "12 %.3g 34", 1.23));
        // And date/time formatting (we assume that all time/date number formatting is done by the
        // same code, so this is representative):
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-08:00"));
        c.setTimeInMillis(0);
        assertEquals("12 \u0661\u0666:\u0660\u0660:\u0660\u0660 34",
                String.format(arabic, "12 %tT 34", c));
        // These shouldn't get localized:
        assertEquals("1234", String.format(arabic, "1234"));
        assertEquals("1234", String.format(arabic, "%s", "1234"));
        assertEquals("1234", String.format(arabic, "%s", 1234));
        assertEquals("2322", String.format(arabic, "%o", 1234));
        assertEquals("4d2", String.format(arabic, "%x", 1234));
        assertEquals("0x1.0p0", String.format(arabic, "%a", 1.0));
    }

    // http://b/27566754
    @NonMts(reason = NonMtsReasons.ICU_VERSION_DEPENDENCY,
        disabledUntilSdk = VersionCodes.VANILLA_ICE_CREAM)
    @Test
    public void test_internationalizedExponent() {
        assertEquals("1E+02", String.format(Locale.ENGLISH, "%.0E", 100.0));
        assertEquals("1e+02", String.format(Locale.ENGLISH, "%.0e", 100.0));

        assertEquals("\u0661\u0623\u0633+\u0660\u0662", String.format(new Locale("ar"), "%.0E", 100.0));
        assertEquals("\u0661\u0623\u0633+\u0660\u0662", String.format(new Locale("ar"), "%.0e", 100.0));

        assertEquals("1\u00d710^+02", String.format(new Locale("et"), "%.0E", 100.0));
        assertEquals("1\u00d710^+02", String.format(new Locale("et"), "%.0e", 100.0));
    }

    // http://b/2301938
    @Test
    public void test_uppercaseConversions() throws Exception {
        // In most locales, the upper-case equivalent of "i" is "I".
        assertEquals("JAKOB ARJOUNI", String.format(Locale.US, "%S", "jakob arjouni"));
        // In Turkish-language locales, there's a dotted capital "i".
        assertEquals("JAKOB ARJOUN\u0130", String.format(new Locale("tr", "TR"), "%S", "jakob arjouni"));
    }

    // Creating a NumberFormat is expensive, so we like to reuse them, but we need to be careful
    // because they're mutable.
    @Test
    public void test_NumberFormat_reuse() throws Exception {
        assertEquals("7.000000 7", String.format("%.6f %d", 7.0, 7));
    }

    @Test
    public void test_grouping() throws Exception {
        // The interesting case is -123, where you might naively output "-,123" if you're just
        // inserting a separator every three characters. The cases where there are three digits
        // before the first separator may also be interesting.
        assertEquals("-1", String.format("%,d", -1));
        assertEquals("-12", String.format("%,d", -12));
        assertEquals("-123", String.format("%,d", -123));
        assertEquals("-1,234", String.format("%,d", -1234));
        assertEquals("-12,345", String.format("%,d", -12345));
        assertEquals("-123,456", String.format("%,d", -123456));
        assertEquals("-1,234,567", String.format("%,d", -1234567));
        assertEquals("-12,345,678", String.format("%,d", -12345678));
        assertEquals("-123,456,789", String.format("%,d", -123456789));
        assertEquals("1", String.format("%,d", 1));
        assertEquals("12", String.format("%,d", 12));
        assertEquals("123", String.format("%,d", 123));
        assertEquals("1,234", String.format("%,d", 1234));
        assertEquals("12,345", String.format("%,d", 12345));
        assertEquals("123,456", String.format("%,d", 123456));
        assertEquals("1,234,567", String.format("%,d", 1234567));
        assertEquals("12,345,678", String.format("%,d", 12345678));
        assertEquals("123,456,789", String.format("%,d", 123456789));
    }

    @Test
    public void test_formatNull() throws Exception {
        // We fast-path %s and %d (with no configuration) but need to make sure we handle the
        // special case of the null argument...
        assertEquals("null", String.format(Locale.US, "%s", (String) null));
        assertEquals("null", String.format(Locale.US, "%d", (Integer) null));
        // ...without screwing up conversions that don't take an argument.
        assertEquals("%", String.format(Locale.US, "%%"));
    }

    // Alleged regression tests for historical bugs. (It's unclear whether the bugs were in
    // BigDecimal or Formatter.)
    @Test
    public void test_BigDecimalFormatting() throws Exception {
        BigDecimal[] input = new BigDecimal[] {
            new BigDecimal("20.00000"),
            new BigDecimal("20.000000"),
            new BigDecimal(".2"),
            new BigDecimal("2"),
            new BigDecimal("-2"),
            new BigDecimal("200000000000000000000000"),
            new BigDecimal("20000000000000000000000000000000000000000000000000")
        };
        String[] output = new String[] {
            "20.00",
            "20.00",
            "0.20",
            "2.00",
            "-2.00",
            "200000000000000000000000.00",
            "20000000000000000000000000000000000000000000000000.00"
        };
        for (int i = 0; i < input.length; ++i) {
            String result = String.format("%.2f", input[i]);
            assertEquals("input=\"" + input[i] + "\", " + ",expected=" + output[i] + ",actual=" + result,
                    output[i], result);
        }
    }

    // https://code.google.com/p/android/issues/detail?id=42936
    @Test
    public void test42936() throws Exception {
        assertEquals("0.00000000000000", String.format("%.15g",0.0d));
    }

    // https://code.google.com/p/android/issues/detail?id=53983
    @Test
    public void test53983() throws Exception {
      checkFormat("00", "H", 00);
      checkFormat( "0", "k", 00);
      checkFormat("12", "I", 00);
      checkFormat("12", "l", 00);

      checkFormat("01", "H", 01);
      checkFormat( "1", "k", 01);
      checkFormat("01", "I", 01);
      checkFormat( "1", "l", 01);

      checkFormat("12", "H", 12);
      checkFormat("12", "k", 12);
      checkFormat("12", "I", 12);
      checkFormat("12", "l", 12);

      checkFormat("13", "H", 13);
      checkFormat("13", "k", 13);
      checkFormat("01", "I", 13);
      checkFormat( "1", "l", 13);

      checkFormat("00", "H", 24);
      checkFormat( "0", "k", 24);
      checkFormat("12", "I", 24);
      checkFormat("12", "l", 24);
    }

    private static void checkFormat(String expected, String pattern, int hour) {
      TimeZone utc = TimeZone.getTimeZone("UTC");

      Calendar c = new GregorianCalendar(utc);
      c.set(2013, Calendar.JANUARY, 1, hour, 00);

      assertEquals(expected, String.format(Locale.US, "%t" + pattern, c));
      assertEquals(expected, String.format(Locale.US, "%T" + pattern, c));
    }

    // http://b/33245708: Some locales have a group separator != '\0' but a default decimal format
    // pattern without grouping (e.g. a group size of zero). This would throw divide by zero when
    // working out where to place the separator.
    @Test
    public void testGroupingSizeZero() {
        Locale localeWithoutGrouping = new Locale("en", "US", "POSIX");
        DecimalFormat decimalFormat =
                (DecimalFormat) NumberFormat.getInstance(localeWithoutGrouping);

        // Confirm the locale is still a good example: it has a group separator, but no grouping in
        // the default decimal format.
        assertEquals(0, decimalFormat.getGroupingSize());
        assertFalse(decimalFormat.isGroupingUsed());
        DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
        assertTrue(symbols.getGroupingSeparator() != '\0');

        Formatter formatter = new Formatter(localeWithoutGrouping);
        formatter.format("%,d", 123456789);
        // No exception expected
    }

    @Test
    public void testConstructor_Ljava_io_OutputStreamLjava_nio_charset_CharsetLjava_util_Locale()
            throws Exception {
        try {
            new Formatter((OutputStream) null, UTF_8, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }

        try (var os = new FileOutputStream(aFile)) {
            new Formatter(os, (Charset) null, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }

        try (var os = new FileOutputStream(aFile)) {
            var formatter = new Formatter(os, UTF_8, (Locale) null);
            assertNull(formatter.locale());
        }

        try (var os = new FileOutputStream(aFile)) {
            var locale = Locale.getDefault();
            var formatter = new Formatter(os, UTF_8, locale);
            assertEquals(locale, formatter.locale());
        }
    }

    @Test
    public void testConstructor_Ljava_io_FileLjava_nio_charset_CharsetLjava_util_Locale()
            throws Exception {
        try {
            new Formatter((File) null, UTF_8, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }

        try {
            new Formatter(aFile, (Charset) null, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }

        var formatter = new Formatter(aFile, UTF_8, (Locale) null);
        assertNull(formatter.locale());

        var locale = Locale.getDefault();
        formatter = new Formatter(aFile, UTF_8, locale);
        assertEquals(locale, formatter.locale());
    }

    @Test
    public void testConstructor_Ljava_lang_StringLjava_nio_charset_CharsetLjava_util_Locale()
            throws Exception {
        try {
            new Formatter((String) null, UTF_8, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }

        try {
            new Formatter(aFile.getName(), (Charset) null, Locale.getDefault());
            fail("Should throw NPE");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    @Test
    public void test_floatWithAlternateForm() {
        // when # flag is set, decimal separator is always present.
        assertEquals("10.", new Formatter(Locale.US).format("%#.0f", 10.12).toString());
    }

    @Test
    public void test_numberSignIsNotAllowed_inGeneralFormat() {
        assertThrows(FormatFlagsConversionMismatchException.class,
                () -> new Formatter(Locale.US).format("%#.1g", 10.1).toString());
    }

    @Test
    @DisableCompatChanges({Formatter.ENABLE_STRICT_FORMATTER_VALIDATION})
    public void zerothIndexIsTreatedAsOrdinaryIndex_whenChecksAreDisabled() {
        assertEquals("x", new Formatter(Locale.US).format("%0$s", "x").toString());
        assertEquals("a a b", new Formatter(Locale.US).format("%s %1$s %0$s", "a", "b").toString());
        assertEquals(
                "string string",
                new Formatter(Locale.US).format("%s %2147483648$s", "string").toString());
        assertThrows(
                MissingFormatArgumentException.class,
                () -> new Formatter(Locale.US).format("%0$s %0$s %0$s", "x").toString());
    }

    @Test
    @DisableCompatChanges({Formatter.ENABLE_STRICT_FORMATTER_VALIDATION})
    public void invalidIndexIsTreatedAsOrdinaryIndex_whenChecksAreDisabled() {
        // In practice it makes no sense to use such a large index - there can't be that many
        // elements in an array anyway. OpenJDK 11 (and older) implementation swallowed exception
        // during these elements parsing and left default values. For index default value was -1,
        // which effectively means relative index a.k.a. re-use argument used for previous format
        // specifier.
        assertEquals(
                "string string",
                new Formatter(Locale.US).format("%s %2147483648$s", "string").toString());
        assertThrows(
                MissingFormatArgumentException.class,
                () -> new Formatter(Locale.US).format("%2147483648$s", "string").toString()
        );
    }

    @Test
    @EnableCompatChanges({Formatter.ENABLE_STRICT_FORMATTER_VALIDATION})
    public void invalidIndicesThrowsException_afterU_whenChecksAreEnabled() {
        var msg = "Checks are done only starting from V. Current SDK=" + VMRuntime.getSdkVersion();
        assumeTrue(msg, VMRuntime.getSdkVersion() >= VersionCodes.VANILLA_ICE_CREAM);
        var formatter = new Formatter(Locale.US);
        var exception = assertThrows(
                IllegalFormatException.class,
                () -> formatter.format("%0$s", "x").toString());

        assertEquals("IllegalFormatArgumentIndexException", exception.getClass().getSimpleName());

        exception = assertThrows(
                IllegalFormatException.class,
                () -> new Formatter(Locale.US).format("%s %2147483648$s", "string").toString());
        assertEquals("IllegalFormatArgumentIndexException", exception.getClass().getSimpleName());
    }

    @Test
    @EnableCompatChanges({Formatter.ENABLE_STRICT_FORMATTER_VALIDATION})
    public void invalidIndicesDontThrow_beforeV_whenChecksAreEnabled() {
        var msg = "Checks are done only starting from V. Current SDK=" + VMRuntime.getSdkVersion();
        assumeTrue(msg, VMRuntime.getSdkVersion() < VersionCodes.VANILLA_ICE_CREAM);

        assertEquals("x", new Formatter(Locale.US).format("%0$s", "x").toString());
        assertEquals("a a b", new Formatter(Locale.US).format("%s %1$s %0$s", "a", "b").toString());
    }

}

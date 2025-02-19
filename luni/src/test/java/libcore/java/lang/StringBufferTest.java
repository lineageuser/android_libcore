/*
 * Copyright (C) 2016 The Android Open Source Project
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

package libcore.java.lang;

import junit.framework.TestCase;

import java.util.Arrays;

public class StringBufferTest extends TestCase {

    public void testChars() {
        StringBuffer s = new StringBuffer("Hello\n\tworld");
        int[] expected = new int[s.length()];
        for (int i = 0; i < s.length(); ++i) {
            expected[i] = (int) s.charAt(i);
        }
        assertTrue(Arrays.equals(expected, s.chars().toArray()));

        // Surrogate code point
        char high = '\uD83D', low = '\uDE02';
        StringBuffer surrogateCP = new StringBuffer().append(new char[]{high, low, low});
        assertTrue(Arrays.equals(new int[]{high, low, low}, surrogateCP.chars().toArray()));
    }

    public void testCodePoints() {
        StringBuffer s = new StringBuffer("Hello\n\tworld");
        int[] expected = new int[s.length()];
        for (int i = 0; i < s.length(); ++i) {
            expected[i] = (int) s.charAt(i);
        }
        assertTrue(Arrays.equals(expected, s.codePoints().toArray()));

        // Surrogate code point
        char high = '\uD83D', low = '\uDE02';
        StringBuffer surrogateCP = new StringBuffer().append(new char[]{high, low, low, '0'});
        assertEquals(Character.toCodePoint(high, low), surrogateCP.codePoints().toArray()[0]);
        assertEquals((int) low, surrogateCP.codePoints().toArray()[1]); // Unmatched surrogate.
        assertEquals((int) '0', surrogateCP.codePoints().toArray()[2]);
    }

    public void testCompareTo() {
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer("a");
        assertTrue(sb1.compareTo(sb2) < 0);
        sb1.append("a");
        assertEquals(0, sb1.compareTo(sb2));

        // Test special character '\0'
        sb2.append('\0');
        assertTrue(sb1.compareTo(sb2) < 0);
        sb1.append('\0');
        assertEquals(0, sb1.compareTo(sb2));
        assertEquals("a\0", sb1.toString());

        // Test a UTF-16 character
        sb2.append('\u0161');
        assertTrue(sb1.compareTo(sb2) < 0);
        sb1.append('\u0161');
        assertEquals(0, sb1.compareTo(sb2));
        assertEquals("a\0\u0161", sb1.toString());

        // Now clear the StringBuffer which has "expanded", and test again.
        sb1.setLength(0);
        assertEquals(0, new StringBuffer().compareTo(sb1));
        sb1.append("a");
        assertEquals(0, new StringBuffer("a").compareTo(sb1));
    }

    // Regression test for b/356007654.
    public void testIndexOf_afterAppendChar() {
        assertIndexOfAfterAppend('Z');
        assertIndexOfAfterAppend(' ');
        assertIndexOfAfterAppend('\u0010');
        assertIndexOfAfterAppend('\u00B0');
        assertIndexOfAfterAppend('\u0080');
        assertIndexOfAfterAppend('\u00ff');
        assertIndexOfAfterAppend('\u0100');
        assertIndexOfAfterAppend('\u201f');
        assertIndexOfAfterAppend('\uffff');
    }

    private static void assertIndexOfAfterAppend(char ch) {
        String str = String.valueOf(ch);

        StringBuffer sb = new StringBuffer("abc");
        sb.append(ch);
        sb.append("123");
        sb.append(ch);
        sb.append("abc");
        assertEquals(3, sb.indexOf(str));
    }
    // Regression test for b/356007654.
    public void testLastIndexOf_afterAppendChar() {
        assertLastIndexOfAfterAppend('Z');
        assertLastIndexOfAfterAppend(' ');
        assertLastIndexOfAfterAppend('\u0010');
        assertLastIndexOfAfterAppend('\u00B0');
        assertLastIndexOfAfterAppend('\u0080');
        assertLastIndexOfAfterAppend('\u00ff');
        assertLastIndexOfAfterAppend('\u0100');
        assertLastIndexOfAfterAppend('\u201f');
        assertLastIndexOfAfterAppend('\uffff');
    }

    private static void assertLastIndexOfAfterAppend(char ch) {
        String str = String.valueOf(ch);

        StringBuffer sb = new StringBuffer("abc");
        sb.append(ch);
        sb.append("123");
        sb.append(ch);
        sb.append("abc");
        assertEquals(7, sb.lastIndexOf(str));
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

package test.java.util.concurrent.tck;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class ThreadLocalTest extends JSR166TestCase {

    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.ThreadLocalTest");
    }
    // public static Test suite() {
    //     return new TestSuite(ThreadLocalTest.class);
    // }

    static ThreadLocal<Integer> tl = new ThreadLocal<Integer>() {
            public Integer initialValue() {
                return one;
            }
        };

    static InheritableThreadLocal<Integer> itl =
        new InheritableThreadLocal<Integer>() {
            protected Integer initialValue() {
                return zero;
            }

            protected Integer childValue(Integer parentValue) {
                return new Integer(parentValue.intValue() + 1);
            }
        };

    /**
     * remove causes next access to return initial value
     */
    @Test
    public void testRemove() {
        assertSame(tl.get(), one);
        tl.set(two);
        assertSame(tl.get(), two);
        tl.remove();
        assertSame(tl.get(), one);
    }

    /**
     * remove in InheritableThreadLocal causes next access to return
     * initial value
     */
    @Test
    public void testRemoveITL() {
        assertSame(itl.get(), zero);
        itl.set(two);
        assertSame(itl.get(), two);
        itl.remove();
        assertSame(itl.get(), zero);
    }

    private class ITLThread extends Thread {
        final int[] x;
        ITLThread(int[] array) { x = array; }
        public void run() {
            Thread child = null;
            if (itl.get().intValue() < x.length - 1) {
                child = new ITLThread(x);
                child.start();
            }
            Thread.yield();

            int threadId = itl.get().intValue();
            for (int j = 0; j < threadId; j++) {
                x[threadId]++;
                Thread.yield();
            }

            if (child != null) { // Wait for child (if any)
                try {
                    child.join();
                } catch (InterruptedException e) {
                    threadUnexpectedException(e);
                }
            }
        }
    }

    /**
     * InheritableThreadLocal propagates generic values.
     */
    @Test
    public void testGenericITL() throws InterruptedException {
        final int threadCount = 10;
        final int[] x = new int[threadCount];
        Thread progenitor = new ITLThread(x);
        progenitor.start();
        progenitor.join();
        for (int i = 0; i < threadCount; i++) {
            assertEquals(i, x[i]);
        }
    }
}

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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// Android-changed: Use JUnit4.
@RunWith(JUnit4.class)
public class AbstractExecutorServiceTest extends JSR166TestCase {
    // Android-changed: Use JUnitCore.main.
    public static void main(String[] args) {
        // main(suite(), args);
        org.junit.runner.JUnitCore.main("test.java.util.concurrent.tck.AbstractExecutorServiceTest");
    }
    // public static Test suite() {
    //     return new TestSuite(AbstractExecutorServiceTest.class);
    // }

    /**
     * A no-frills implementation of AbstractExecutorService, designed
     * to test the submit methods only.
     */
    static class DirectExecutorService extends AbstractExecutorService {
        public void execute(Runnable r) { r.run(); }
        public void shutdown() { shutdown = true; }
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.EMPTY_LIST;
        }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return isShutdown(); }
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isShutdown();
        }
        private volatile boolean shutdown = false;
    }

    /**
     * execute(runnable) runs it to completion
     */
    @Test
    public void testExecuteRunnable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        final AtomicBoolean done = new AtomicBoolean(false);
        Future<?> future = e.submit(new CheckedRunnable() {
            public void realRun() {
                done.set(true);
            }});
        assertNull(future.get());
        assertNull(future.get(0, MILLISECONDS));
        assertTrue(done.get());
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    /**
     * Completed submit(callable) returns result
     */
    @Test
    public void testSubmitCallable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<String> future = e.submit(new StringTask());
        String result = future.get();
        assertSame(TEST_STRING, result);
    }

    /**
     * Completed submit(runnable) returns successfully
     */
    @Test
    public void testSubmitRunnable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<?> future = e.submit(new NoOpRunnable());
        future.get();
        assertTrue(future.isDone());
    }

    /**
     * Completed submit(runnable, result) returns result
     */
    @Test
    public void testSubmitRunnable2() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
        String result = future.get();
        assertSame(TEST_STRING, result);
    }

    /**
     * A submitted privileged action runs to completion
     */
    @Test
    @Ignore("Not run in Android")
    public void testSubmitPrivilegedAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future future = e.submit(Executors.callable(new PrivilegedAction() {
                    public Object run() {
                        return TEST_STRING;
                    }}));

                assertSame(TEST_STRING, future.get());
            }};

        runWithPermissions(r,
                           new RuntimePermission("getClassLoader"),
                           new RuntimePermission("setContextClassLoader"),
                           new RuntimePermission("modifyThread"));
    }

    /**
     * A submitted privileged exception action runs to completion
     */
    @Test
    @Ignore("Not run in Android")
    public void testSubmitPrivilegedExceptionAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future future = e.submit(Executors.callable(new PrivilegedExceptionAction() {
                    public Object run() {
                        return TEST_STRING;
                    }}));

                assertSame(TEST_STRING, future.get());
            }};

        runWithPermissions(r);
    }

    /**
     * A submitted failed privileged exception action reports exception
     */
    @Test
    @Ignore("Not run in Android")
    public void testSubmitFailedPrivilegedExceptionAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future future = e.submit(Executors.callable(new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        throw new IndexOutOfBoundsException();
                    }}));

                try {
                    future.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    assertTrue(success.getCause() instanceof IndexOutOfBoundsException);
                }}};

        runWithPermissions(r);
    }

    /**
     * execute(null runnable) throws NPE
     */
    @Test
    public void testExecuteNullRunnable() {
        ExecutorService e = new DirectExecutorService();
        try {
            e.submit((Runnable) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * submit(null callable) throws NPE
     */
    @Test
    public void testSubmitNullCallable() {
        ExecutorService e = new DirectExecutorService();
        try {
            e.submit((Callable) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * submit(callable).get() throws InterruptedException if interrupted
     */
    @Test
    public void testInterruptedSubmit() throws InterruptedException {
        final CountDownLatch submitted    = new CountDownLatch(1);
        final CountDownLatch quittingTime = new CountDownLatch(1);
        final Callable<Void> awaiter = new CheckedCallable<Void>() {
            public Void realCall() throws InterruptedException {
                assertTrue(quittingTime.await(2*LONG_DELAY_MS, MILLISECONDS));
                return null;
            }};
        final ExecutorService p
            = new ThreadPoolExecutor(1,1,60, TimeUnit.SECONDS,
                                     new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, quittingTime)) {
            Thread t = newStartedThread(new CheckedInterruptedRunnable() {
                public void realRun() throws Exception {
                    Future<Void> future = p.submit(awaiter);
                    submitted.countDown();
                    future.get();
                }});

            await(submitted);
            t.interrupt();
            awaitTermination(t);
        }
    }

    /**
     * get of submit(callable) throws ExecutionException if callable
     * throws exception
     */
    @Test
    public void testSubmitEE() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   60, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            Callable c = new Callable() {
                public Object call() { throw new ArithmeticException(); }};
            try {
                p.submit(c).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof ArithmeticException);
            }
        }
    }

    /**
     * invokeAny(null) throws NPE
     */
    @Test
    public void testInvokeAny1() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(empty collection) throws IAE
     */
    @Test
    public void testInvokeAny2() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * invokeAny(c) throws NPE if c has null elements
     */
    @Test
    public void testInvokeAny3() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<Long>> l = new ArrayList<>();
            l.add(new Callable<Long>() {
                      public Long call() { throw new ArithmeticException(); }});
            l.add(null);
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(c) throws ExecutionException if no task in c completes
     */
    @Test
    public void testInvokeAny4() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAny(c) returns result of some task in c if at least one completes
     */
    @Test
    public void testInvokeAny5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l);
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAll(null) throws NPE
     */
    @Test
    public void testInvokeAll1() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAll(empty collection) returns empty collection
     */
    @Test
    public void testInvokeAll2() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r = e.invokeAll(new ArrayList<Callable<String>>());
            assertTrue(r.isEmpty());
        }
    }

    /**
     * invokeAll(c) throws NPE if c has null elements
     */
    @Test
    public void testInvokeAll3() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws exception on failed task
     */
    @Test
    public void testInvokeAll4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAll(c) returns results of all completed tasks in c
     */
    @Test
    public void testInvokeAll5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAny(null) throws NPE
     */
    @Test
    public void testTimedInvokeAny1() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null, MEDIUM_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(null time unit) throws NPE
     */
    @Test
    public void testTimedInvokeAnyNullTimeUnit() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAny(l, MEDIUM_DELAY_MS, null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(empty collection) throws IAE
     */
    @Test
    public void testTimedInvokeAny2() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>(),
                            MEDIUM_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws NPE if c has null elements
     */
    @Test
    public void testTimedInvokeAny3() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<Long>> l = new ArrayList<>();
            l.add(new Callable<Long>() {
                      public Long call() { throw new ArithmeticException(); }});
            l.add(null);
            try {
                e.invokeAny(l, MEDIUM_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws ExecutionException if no task completes
     */
    @Test
    public void testTimedInvokeAny4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAny(c) returns result of some task in c
     */
    @Test
    public void testTimedInvokeAny5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
            assertSame(TEST_STRING, result);
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAll(null) throws NPE
     */
    @Test
    public void testTimedInvokeAll1() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null, MEDIUM_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(null time unit) throws NPE
     */
    @Test
    public void testTimedInvokeAllNullTimeUnit() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAll(l, MEDIUM_DELAY_MS, null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(empty collection) returns empty collection
     */
    @Test
    public void testTimedInvokeAll2() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r = e.invokeAll(new ArrayList<Callable<String>>(), MEDIUM_DELAY_MS, MILLISECONDS);
            assertTrue(r.isEmpty());
        }
    }

    /**
     * timed invokeAll(c) throws NPE if c has null elements
     */
    @Test
    public void testTimedInvokeAll3() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l, MEDIUM_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws exception on failed task
     */
    @Test
    public void testTimedInvokeAll4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * timed invokeAll(c) returns results of all completed tasks in c
     */
    @Test
    public void testTimedInvokeAll5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAll cancels tasks not completed by timeout
     */
    @Test
    public void testTimedInvokeAll6() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            for (long timeout = timeoutMillis();;) {
                List<Callable<String>> tasks = new ArrayList<>();
                tasks.add(new StringTask("0"));
                tasks.add(Executors.callable(possiblyInterruptedRunnable(timeout),
                                             TEST_STRING));
                tasks.add(new StringTask("2"));
                long startTime = System.nanoTime();
                List<Future<String>> futures =
                    e.invokeAll(tasks, timeout, MILLISECONDS);
                assertEquals(tasks.size(), futures.size());
                assertTrue(millisElapsedSince(startTime) >= timeout);
                for (Future future : futures)
                    assertTrue(future.isDone());
                try {
                    assertEquals("0", futures.get(0).get());
                    assertEquals(TEST_STRING, futures.get(1).get());
                } catch (CancellationException retryWithLongerTimeout) {
                    // unusual delay before starting second task
                    timeout *= 2;
                    if (timeout >= LONG_DELAY_MS / 2)
                        fail("expected exactly one task to be cancelled");
                    continue;
                }
                assertTrue(futures.get(2).isCancelled());
                break;
            }
        }
    }

}

// from gee.cs.oswego.edu/home/jsr166/jsr166

package jsr166tests.jtreg.util.concurrent.ThreadPoolExecutor;

/*
 * Copyright 2005 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * @test
 * @bug 6277663
 * @summary Test TPE extensibility framework
 * @author Martin Buchholz
 */

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Custom {
    static volatile int passed = 0, failed = 0;
    static void pass() { passed++; }
    static void fail() { failed++; Thread.dumpStack(); }
    static void unexpected(Throwable t) { failed++; t.printStackTrace(); }
    static void check(boolean cond) { if (cond) pass(); else fail(); }
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y); fail(); }}


    private static class CustomTask<V> extends FutureTask<V> {
        public final static AtomicInteger births = new AtomicInteger(0);
        CustomTask(Callable<V> c) { super(c); births.getAndIncrement(); }
        CustomTask(Runnable r, V v) { super(r, v); births.getAndIncrement(); }
    }

    private static class CustomTPE extends ThreadPoolExecutor {
        CustomTPE() {
            super(threadCount, threadCount,
                  30, TimeUnit.MILLISECONDS,
                  new ArrayBlockingQueue<Runnable>(2*threadCount));
        }
        protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
            return new CustomTask<V>(c);
        }
        protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
            return new CustomTask<V>(r, v);
        }
    }

    private static class CustomSTPE extends ScheduledThreadPoolExecutor {
        public final static AtomicInteger decorations = new AtomicInteger(0);
        CustomSTPE() {
            super(threadCount);
        }
        protected <V> RunnableScheduledFuture<V> decorateTask(
            Runnable r, RunnableScheduledFuture<V> task) {
            decorations.getAndIncrement();
            return task;
        }
        protected <V> RunnableScheduledFuture<V> decorateTask(
            Callable<V> c, RunnableScheduledFuture<V> task) {
            decorations.getAndIncrement();
            return task;
        }
    }

    static int countExecutorThreads() {
        Thread[] threads = new Thread[Thread.activeCount()+100];
        Thread.enumerate(threads);
        int count = 0;
        for (Thread t : threads)
            if (t != null && t.getName().matches("pool-[0-9]+-thread-[0-9]+"))
                count++;
        return count;
    }

    private final static int threadCount = 10;

    public static void main(String[] args) throws Throwable {
        CustomTPE tpe = new CustomTPE();
        equal(tpe.getCorePoolSize(), threadCount);
        equal(countExecutorThreads(), 0);
        for (int i = 0; i < threadCount; i++)
            tpe.submit(new Runnable() { public void run() {}});
        equal(countExecutorThreads(), threadCount);
        equal(CustomTask.births.get(), threadCount);
        tpe.shutdown();
        tpe.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Thread.sleep(10);
        equal(countExecutorThreads(), 0);

        CustomSTPE stpe = new CustomSTPE();
        for (int i = 0; i < threadCount; i++)
            stpe.submit(new Runnable() { public void run() {}});
        equal(CustomSTPE.decorations.get(), threadCount);
        equal(countExecutorThreads(), threadCount);
        stpe.shutdown();
        stpe.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        Thread.sleep(10);
        equal(countExecutorThreads(), 0);

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
}

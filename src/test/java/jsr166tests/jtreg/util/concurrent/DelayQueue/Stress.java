// from gee.cs.oswego.edu/home/jsr166/jsr166

package jsr166tests.jtreg.util.concurrent.DelayQueue;

/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.*;

/**
 * This is not a regression test, but a stress benchmark test for
 * 6609775: Reduce context switches in DelayQueue due to signalAll
 *
 * This runs in the same wall clock time, but much reduced cpu time,
 * with the changes for 6609775.
 */
public class Stress {

    public static void main(String[] args) throws Throwable {

        final DelayQueue<Delayed> q = new DelayQueue<Delayed>();
        final long t0 = System.nanoTime();
        for (long i = 0; i < 1000; i++) {
            final long expiry = t0 + i*10L*1000L*1000L;
            q.add(new Delayed() {
                    public long getDelay(TimeUnit unit) {
                        return unit.convert(expiry - System.nanoTime(),
                                            NANOSECONDS);
                    }
                    public int compareTo(Delayed x) {
                        long d = getDelay(NANOSECONDS)
                            - x.getDelay(NANOSECONDS);
                        return d < 0 ? -1 : d > 0 ? 1 : 0; }});
        }

        for (int i = 0; i < 300; i++)
            new Thread() { public void run() {
                try {
                    while (!q.isEmpty())
                        q.poll(10L, TimeUnit.SECONDS);
                } catch (Throwable t) { t.printStackTrace(); }
            }}.start();
    }
}

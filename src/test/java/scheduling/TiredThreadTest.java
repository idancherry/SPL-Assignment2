package scheduling;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredThreadTest {

    @Test
    void newTask_null_throws() {
        TiredThread t = new TiredThread(1, 1.0);
        assertThrows(NullPointerException.class, () -> t.newTask(null));
    }

    @Test
    void newTask_beforeStart_acceptsAndRunsTask() throws Exception {
        TiredThread t = new TiredThread(1, 1.0);
        AtomicInteger x = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(1);

        t.newTask(() -> {
            x.incrementAndGet();
            done.countDown();
        });

        t.start();
        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals(1, x.get());

        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void newTask_whenBusy_throws() throws Exception {
        TiredThread t = new TiredThread(1, 1.0);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        t.start();

        t.newTask(() -> {
            started.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));

        release.countDown();
        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void newTask_whenQueueAlreadyHasTask_throws() throws Exception {
        TiredThread t = new TiredThread(1, 1.0);

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        t.newTask(() -> {
            firstStarted.countDown();
            try {
                releaseFirst.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));

        t.start();
        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

        releaseFirst.countDown();
        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void shutdown_stopsThread() throws Exception {
        TiredThread t = new TiredThread(7, 2.0);
        t.start();
        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void getWorkerId_returnsId() {
        TiredThread t = new TiredThread(42, 1.0);
        assertEquals(42, t.getWorkerId());
    }

    @Test
    void timeUsed_increasesAfterRunningBlockingTask() throws Exception {
        TiredThread t = new TiredThread(1, 1.0);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        t.start();

        t.newTask(() -> {
            started.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        assertTrue(started.await(2, TimeUnit.SECONDS));
        Thread.sleep(10);

        release.countDown();

        CountDownLatch done = new CountDownLatch(1);
        t.newTask(done::countDown);
        assertTrue(done.await(2, TimeUnit.SECONDS));

        long used = t.getTimeUsed();
        assertTrue(used > 0);

        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void timeIdle_increasesAfterWaitingThenSubmittingTask() throws Exception {
        TiredThread t = new TiredThread(1, 1.0);
        t.start();

        Thread.sleep(20);

        CountDownLatch done = new CountDownLatch(1);
        t.newTask(done::countDown);
        assertTrue(done.await(2, TimeUnit.SECONDS));

        long idle = t.getTimeIdle();
        assertTrue(idle > 0);

        t.shutdown();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void compareTo_ordersByFatigue_afterDifferentWork() throws Exception {
        TiredThread a = new TiredThread(1, 1.0);
        TiredThread b = new TiredThread(2, 2.0);

        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch aRelease = new CountDownLatch(1);

        a.start();
        b.start();

        a.newTask(() -> {
            aStarted.countDown();
            try {
                aRelease.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        });

        assertTrue(aStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(10);
        aRelease.countDown();

        CountDownLatch aDone = new CountDownLatch(1);
        a.newTask(aDone::countDown);
        assertTrue(aDone.await(2, TimeUnit.SECONDS));

        CountDownLatch bDone = new CountDownLatch(1);
        b.newTask(bDone::countDown);
        assertTrue(bDone.await(2, TimeUnit.SECONDS));

        int cmp = a.compareTo(b);
        assertNotEquals(0, cmp);

        a.shutdown();
        b.shutdown();
        a.join(2000);
        b.join(2000);
    }
}

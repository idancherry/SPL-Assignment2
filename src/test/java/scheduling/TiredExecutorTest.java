package scheduling;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    @Test
    void submit_null_throws() {
        TiredExecutor ex = new TiredExecutor(1);
        try {
            assertThrows(NullPointerException.class, () -> ex.submit(null));
        } finally {
            try {
                ex.shutdown();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    void submit_runsTask() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        CountDownLatch done = new CountDownLatch(1);

        try {
            ex.submit(done::countDown);
            assertTrue(done.await(2, TimeUnit.SECONDS));
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void submitAll_runsAllTasks() throws Exception {
        TiredExecutor ex = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        try {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(counter::incrementAndGet);
            }

            ex.submitAll(tasks);
            assertEquals(50, counter.get());
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void submitAll_waitsUntilAllFinish() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        try {
            List<Runnable> tasks = new ArrayList<>();
            tasks.add(() -> {
                started.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                counter.incrementAndGet();
            });
            tasks.add(() -> {
                started.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                counter.incrementAndGet();
            });

            Thread t = new Thread(() -> ex.submitAll(tasks));
            t.start();

            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertEquals(0, counter.get());

            release.countDown();

            t.join(2000);
            assertFalse(t.isAlive());
            assertEquals(2, counter.get());
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void submit_taskThrows_stillAllowsFurtherTasks() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);

        try {
            List<Runnable> tasks = new ArrayList<>();

            tasks.add(() -> { throw new RuntimeException("submit task failed"); });

            AtomicInteger ok = new AtomicInteger(0);
            tasks.add(ok::incrementAndGet);
            tasks.add(ok::incrementAndGet);

            ex.submitAll(tasks);

            assertEquals(2, ok.get());
        } finally {
            ex.shutdown();
        }
    }



    @Test
    void getWorkerReport_returnsNonEmptyString() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        try {
            String rep = ex.getWorkerReport();
            assertNotNull(rep);
            assertTrue(rep.contains("Worker name="));
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void shutdown_terminatesWorkers() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        ex.shutdown();
        String rep = ex.getWorkerReport();
        assertNotNull(rep);
    }
}

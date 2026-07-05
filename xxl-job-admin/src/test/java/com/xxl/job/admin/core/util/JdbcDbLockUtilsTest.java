package com.xxl.job.admin.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractTest;
import jakarta.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings("all")
public class JdbcDbLockUtilsTest extends AbstractTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanLockTable() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_lock");
    }

    @Test
    public void shouldExecuteRunnableWithLock() {
        AtomicInteger counter = new AtomicInteger(0);
        JdbcDbLockUtils.executeWithDbLock("h2-execute-lock", true, true, counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    public void shouldInsertLockRowWhenNotExists() throws InterruptedException {
        JdbcDbLockUtils.executeWithDbLock("h2-insert-lock", true, true, () -> {});

        for (int i = 0; i < 3; i++) {

            try {
                Integer count = jdbcTemplate.queryForObject(
                        "select count(1) from xxl_job_lock where lock_name = ?", Integer.class, "h2-insert-lock");
                assertEquals(1, count);
            } catch (AssertionError e) {
                if (i == 2) {
                    throw e;
                }
                Thread.sleep(100);
            }
        }
    }

    @Test
    public void shouldAllowConcurrentExecutionForDifferentLocks() throws InterruptedException {
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            runWithLock("h2-diff-lock-a", active, maxActive, () -> sleepQuietly(200));
            done.countDown();
        });
        Thread t2 = new Thread(() -> {
            runWithLock("h2-diff-lock-b", active, maxActive, () -> sleepQuietly(200));
            done.countDown();
        });

        t1.start();
        t2.start();
        assertTrue(done.await(5, TimeUnit.SECONDS), "both threads should finish");
        assertEquals(2, maxActive.get(), "different locks should execute concurrently");
    }

    @Test
    public void shouldSerializeExecutionForSameLock() throws InterruptedException {
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);
        CountDownLatch thread1Started = new CountDownLatch(1);
        CountDownLatch thread1CanFinish = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            runWithLock("h2-same-lock", active, maxActive, () -> {
                thread1Started.countDown();
                awaitQuietly(thread1CanFinish);
            });
            done.countDown();
        });

        Thread t2 = new Thread(() -> {
            awaitQuietly(thread1Started);
            runWithLock("h2-same-lock", active, maxActive, () -> {});
            done.countDown();
        });

        t1.start();
        t2.start();
        assertTrue(thread1Started.await(2, TimeUnit.SECONDS), "thread1 should start first");
        Thread.sleep(100);
        thread1CanFinish.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "both threads should finish");
        assertEquals(1, maxActive.get(), "same lock should serialize execution");
    }

    @Test
    public void shouldSerializeExecutionForSameLock2() throws InterruptedException {
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);
        CountDownLatch thread1Started = new CountDownLatch(1);
        CountDownLatch thread1CanFinish = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        jdbcTemplate.execute("INSERT INTO xxl_job_lock (lock_name) VALUES ('h2-same-lock2')");
        Thread t1 = new Thread(() -> {
            runWithLock("h2-same-lock2", active, maxActive, () -> {
                thread1Started.countDown();
                awaitQuietly(thread1CanFinish);
            });
            done.countDown();
        });

        Thread t2 = new Thread(() -> {
            awaitQuietly(thread1Started);
            runWithLock("h2-same-lock2", active, maxActive, () -> {});
            done.countDown();
        });

        t1.start();
        t2.start();
        assertTrue(thread1Started.await(2, TimeUnit.SECONDS), "thread1 should start first");
        Thread.sleep(100);
        thread1CanFinish.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "both threads should finish");
        assertEquals(1, maxActive.get(), "same lock should serialize execution");
    }

    private void runWithLock(String lockName, AtomicInteger active, AtomicInteger maxActive, Runnable withinLock) {
        JdbcDbLockUtils.executeWithDbLock(lockName, true, true, () -> {
            int current = active.incrementAndGet();
            maxActive.updateAndGet(max -> Math.max(max, current));
            try {
                withinLock.run();
            } finally {
                active.decrementAndGet();
            }
        });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

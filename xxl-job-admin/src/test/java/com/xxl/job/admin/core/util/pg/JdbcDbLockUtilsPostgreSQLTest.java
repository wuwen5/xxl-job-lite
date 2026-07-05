package com.xxl.job.admin.core.util.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.util.JdbcDbLockUtils;
import jakarta.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings("all")
public class JdbcDbLockUtilsPostgreSQLTest extends AbstractPostgreSQLTest {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanLockTable() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_lock RESTART IDENTITY CASCADE");
    }

    @Test
    public void shouldExecuteRunnableWithLock() {
        AtomicInteger counter = new AtomicInteger(0);
        JdbcDbLockUtils.executeWithDbLock("pg-execute-lock", true, true, counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    public void shouldInsertLockRowWhenNotExists() {
        JdbcDbLockUtils.executeWithDbLock("pg-insert-lock", true, true, () -> {});

        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from xxl_job_lock where lock_name = ?", Integer.class, "pg-insert-lock");
        assertEquals(1, count);
    }

    @Test
    public void shouldAllowConcurrentExecutionForDifferentLocks() throws InterruptedException {
        AtomicInteger active = new AtomicInteger(0);
        AtomicInteger maxActive = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            runWithLock("pg-diff-lock-a", active, maxActive, () -> sleepQuietly(200));
            done.countDown();
        });
        Thread t2 = new Thread(() -> {
            runWithLock("pg-diff-lock-b", active, maxActive, () -> sleepQuietly(200));
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
            runWithLock("pg-same-lock", active, maxActive, () -> {
                thread1Started.countDown();
                awaitQuietly(thread1CanFinish);
            });
            done.countDown();
        });

        Thread t2 = new Thread(() -> {
            awaitQuietly(thread1Started);
            runWithLock("pg-same-lock", active, maxActive, () -> {});
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

        jdbcTemplate.execute("INSERT INTO xxl_job_lock (lock_name) VALUES ('pg-same-lock2')");
        Thread t1 = new Thread(() -> {
            runWithLock("pg-same-lock2", active, maxActive, () -> {
                thread1Started.countDown();
                awaitQuietly(thread1CanFinish);
            });
            done.countDown();
        });

        Thread t2 = new Thread(() -> {
            awaitQuietly(thread1Started);
            runWithLock("pg-same-lock2", active, maxActive, () -> {});
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
    public void shouldReturnImmediatelyWhenNowaitCannotAcquireLock() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger(0);

        CountDownLatch t1Start = new CountDownLatch(1);
        CountDownLatch t1Hold = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        // thread1: 持有锁
        Thread t1 = new Thread(() -> {
            JdbcDbLockUtils.executeWithDbLock("pg-nowait-lock", true, true, () -> {
                t1Start.countDown();
                try {
                    t1Hold.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
            });
            done.countDown();
        });

        // thread2: NOWAIT 尝试获取锁
        Thread t2 = new Thread(() -> {
            try {
                t1Start.await(2, TimeUnit.SECONDS);

                JdbcDbLockUtils.executeWithDbLock(
                        "pg-nowait-lock",
                        false,
                        false, // ⭐ NOWAIT
                        counter::incrementAndGet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(200);
        t1Hold.countDown();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, counter.get(), "NOWAIT should not execute runnable");
    }

    @Test
    public void shouldRollbackWhenRunnableThrowsException() throws InterruptedException {

        AtomicBoolean secondExecuted = new AtomicBoolean(false);

        CountDownLatch t1Start = new CountDownLatch(1);
        CountDownLatch t1Hold = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                JdbcDbLockUtils.executeWithDbLock("pg-exception-lock", true, true, () -> {
                    t1Start.countDown();
                    try {
                        t1Hold.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("boom");
                });
            } catch (Exception ignored) {
            } finally {
                done.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                t1Start.await(2, TimeUnit.SECONDS);

                JdbcDbLockUtils.executeWithDbLock("pg-exception-lock", false, true, () -> {
                    secondExecuted.set(true);
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(200);
        t1Hold.countDown();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(secondExecuted.get(), "lock should be released after rollback");
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

    @Test
    public void shouldHandleConcurrentInsertLockGracefully() throws InterruptedException {

        int threadCount = 5;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                        try {
                            start.await();

                            JdbcDbLockUtils.executeWithDbLock(
                                    "pg-duplicate-lock",
                                    true, // insertLock=true
                                    true,
                                    success::incrementAndGet);

                        } catch (Exception e) {
                            // ignore
                        } finally {
                            done.countDown();
                        }
                    })
                    .start();
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from xxl_job_lock where lock_name=?", Integer.class, "pg-duplicate-lock");

        assertEquals(1, count, "only one lock row should exist");
        assertEquals(threadCount, success.get(), "all threads should complete");
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

package com.xxl.job.admin.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuwen
 */
@Slf4j
public final class JdbcDbLockUtils {

    private JdbcDbLockUtils() {}

    private static final String INSERT_LOCK_SQL = "insert into xxl_job_lock (lock_name) "
            + "select ? from dual where not exists " + "(select 1 from xxl_job_lock where lock_name = ?)";

    private static final String LOCK_SQL = "select lock_name from xxl_job_lock where lock_name = ? for update";

    public static void executeWithDbLock(DataSource dataSource, String lockName, Runnable run) {
        executeWithDbLock(dataSource, lockName, false, true, run);
    }

    public static void executeWithDbLock(
            DataSource dataSource, String lockName, boolean insertLock, boolean waitForLock, Runnable run) {
        Connection conn = null;
        PreparedStatement insertPs = null;
        PreparedStatement lockPs = null;
        Boolean originAutoCommit = null;

        try {
            conn = dataSource.getConnection();
            originAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            if (insertLock) {
                // 1. ensure lock row exists
                insertPs = conn.prepareStatement(INSERT_LOCK_SQL);
                insertPs.setString(1, lockName);
                insertPs.setString(2, lockName);
                insertPs.executeUpdate();
            }

            // 2. acquire row lock
            lockPs = conn.prepareStatement(LOCK_SQL + (waitForLock ? "" : " nowait"));
            lockPs.setString(1, lockName);
            lockPs.execute();

            // ===== lock start =====
            try {
                run.run();
            } finally {
                conn.commit();
            }
            // ===== lock end =====
        } catch (SQLException e) {
            if (isLockNotAvailable(e)) {
                log.info("executeWithDbLock could not acquire lock, lockName={}", lockName);
            } else {
                log.warn("executeWithDbLock sql error, lockName={}", lockName, e);
                rollbackQuietly(conn);
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            log.error("executeWithDbLock error, lockName={}", lockName, e);
            rollbackQuietly(conn);
            throw new RuntimeException(e);
        } finally {
            restoreAutoCommit(conn, originAutoCommit);
            closeQuietly(lockPs);
            closeQuietly(insertPs);
            closeQuietly(conn);
        }
    }

    static boolean isLockNotAvailable(SQLException e) {
        int code = e.getErrorCode();
        String state = e.getSQLState();

        // Oracle
        return code == 54
                // MySQL
                || code == 1205
                // PostgreSQL
                || "55P03".equals(state);
    }

    // ===== helper methods =====

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private static void restoreAutoCommit(Connection conn, Boolean originAutoCommit) {
        if (conn != null && originAutoCommit != null) {
            try {
                conn.setAutoCommit(originAutoCommit);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}

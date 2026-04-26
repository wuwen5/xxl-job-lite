package com.xxl.job.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * file tool
 *
 * @author xuxueli 2017-12-29 17:56:48
 */
@Slf4j
public class FileUtil {

    /**
     * delete recursively
     *
     * @param root
     * @return
     */
    public static boolean deleteRecursively(File root) {
        if (root != null && root.exists()) {
            if (root.isDirectory()) {
                File[] children = root.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            return root.delete();
        }
        return false;
    }

    public static void writeFileContent(File file, byte[] data) {

        // file
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        // append file content
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static byte[] readFileContent(File file) {
        long filelength = file.length();
        byte[] filecontent = new byte[(int) filelength];

        try (FileInputStream in = new FileInputStream(file)) {
            in.read(filecontent);
            return filecontent;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}

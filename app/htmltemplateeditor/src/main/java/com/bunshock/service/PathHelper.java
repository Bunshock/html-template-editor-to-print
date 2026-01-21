package com.bunshock.service;

import java.io.File;

public class PathHelper {
    public static File resolveFullPath(File baseDir, String relativePath) {
        if (baseDir == null || relativePath == null) {
            return null;
        }
        return baseDir.toPath().resolve(relativePath).toFile();
    }
}

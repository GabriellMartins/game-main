package com.minecraft.core.bukkit.util.compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WinRAR {

    private static final char WINDOWS_SEPARATOR = '\\';
    private static final char UNIX_SEPARATOR = '/';

    public static void make(File source, File output, Predicate<Path> predicate) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        } else if (!source.isDirectory()) {
            throw new IllegalArgumentException("Source is not directory");
        }

        final Path sourcePath = source.toPath();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output))) {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!sourcePath.equals(dir)) {
                        if (predicate != null && !predicate.test(dir))
                            return FileVisitResult.SKIP_SUBTREE;
                        ZipEntry ze = new ZipEntry(relativize(sourcePath, dir) + UNIX_SEPARATOR);
                        ze.setCreationTime(attrs.creationTime());
                        ze.setLastAccessTime(attrs.lastAccessTime());
                        ze.setLastModifiedTime(attrs.lastModifiedTime());
                        zos.putNextEntry(ze);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (predicate == null || predicate.test(file)) {
                        ZipEntry ze = new ZipEntry(relativize(sourcePath, file));
                        ze.setCreationTime(attrs.creationTime());
                        ze.setLastAccessTime(attrs.lastAccessTime());
                        ze.setLastModifiedTime(attrs.lastModifiedTime());
                        zos.putNextEntry(ze);
                        zos.write(Files.readAllBytes(file));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean unzip(File zip, File output) {
        if (zip == null) {
            throw new IllegalArgumentException("com.minecraft.flame.hungergames.file.Zip file cannot be null");
        } else if (!zip.isFile()) {
            throw new IllegalArgumentException("com.minecraft.flame.hungergames.file.Zip file not exists");
        }

        if (output == null) {
            throw new IllegalArgumentException("Output cannot be null");
        } else if (!output.isDirectory()) {
            if (output.exists()) {
                throw new IllegalArgumentException("Output is not directory");
            } else if (!output.mkdir()) {
                throw new RuntimeException("NÃ£o foi possível criar a pasta");
            }
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry ze;

            while ((ze = zis.getNextEntry()) != null) {
                Path path = Paths.get(output.getPath() + File.separator + separatorsToSystem(ze.getName()));

                if (ze.isDirectory()) {
                    if (!Files.exists(path)) {
                        Files.createDirectory(path);
                    }
                } else {
                    Files.copy(zis, path, StandardCopyOption.REPLACE_EXISTING);
                }

                Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(ze.getLastModifiedTime(),
                        ze.getLastAccessTime(), ze.getCreationTime());
            }

            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    private static String relativize(Path start, Path end) {
        return separatorsToUnix(start.relativize(end).toString());
    }

    public static boolean isSystemWindows() {
        return File.separatorChar == WINDOWS_SEPARATOR;
    }

    private static String separatorsToUnix(final String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    private static String separatorsToWindows(final String path) {
        if (path == null || path.indexOf(UNIX_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR);
    }

    private static String separatorsToSystem(final String path) {
        if (path == null) {
            return null;
        }
        return isSystemWindows() ? separatorsToWindows(path) : separatorsToUnix(path);
    }
}


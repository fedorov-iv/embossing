package ru.somebank.embossing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /**
     * Создает директорию, если отсутствует
     *
     * @param dir Директория
     * @throws IOException
     */
    public static void createDirIfNotExist(File dir) throws IOException {
        if (!dir.exists()) {
            boolean success = dir.mkdir();
            if (success) {
                log.info("Folder " + dir.getPath() + " created successfully");
            } else {
                throw new IOException("Could not create " + dir.getPath() + " folder");
            }
        }
    }

    /**
     * Возвращает текущую дату в виде строки
     *
     * @return Строка даты
     */
    public static String currentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd");
        return sdf.format(new Date());
    }

    /**
     * Возвращает текущую дату и время в виде строки
     *
     * @return Строка даты
     */
    public static String currentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMdd-HHmmss");
        return sdf.format(new Date());
    }

    /**
     * Архивирует файл или директорию
     *
     * @param fileToZip
     * @param zipOut
     * @throws IOException
     */
    public static void zipFile(File fileToZip, ZipOutputStream zipOut) throws IOException {

        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileToZip.getName().endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileToZip.getName() + File.separator));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static void zip(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    private static void copy(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            copy(in, out);
        } finally {
            out.close();
        }
    }


    public static void cleanEmbossingFilesFromDirectory(File dir) {
        File[] files = dir.listFiles();

        Arrays.stream(files).filter(f -> f.getName().endsWith(".out") || f.getName().endsWith(".csv")).forEach(f -> {

            log.info("Deleting {}", f.getAbsolutePath());
            boolean success = f.delete();
            if (success)
                log.info("File {} deleted successfully", f.getAbsolutePath());
        });

    }
}

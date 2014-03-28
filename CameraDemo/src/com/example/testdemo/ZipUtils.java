
package com.example.testdemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    /**
     * 把 sourceDir 目录下的所有文件进行 zip 格式的压缩，保存为指定 zip 文件
     * 
     * @param sourceDir
     * @param zipFile
     */
    public static void pack(String sourceDir, String zipFile) {
        OutputStream os;
        try {
            os = new FileOutputStream(zipFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            ZipOutputStream zos = new ZipOutputStream(bos);

            File file = new File(sourceDir);

            String basePath = null;
            if (file.isDirectory()) {
                basePath = file.getPath();
            } else {
                basePath = file.getParent();
            }

            zipFile(file, basePath, zos);

            zos.closeEntry();
            zos.close();
        } catch (Exception e) {
            //
            e.printStackTrace();
        }
    }

    /**
     * @param source
     * @param basePath
     * @param zos
     * @throws IOException
     */
    private static void zipFile(File source, String basePath, ZipOutputStream zos) {
        File[] files = new File[0];

        if (source.isDirectory()) {
            files = source.listFiles();
        } else {
            files = new File[1];
            files[0] = source;
        }

        String pathName;
        byte[] buf = new byte[1024];
        int length = 0;
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    pathName = file.getPath().substring(basePath.length() + 1) + Constances.SLASH;
                    zos.putNextEntry(new ZipEntry(pathName));
                    zipFile(file, basePath, zos);
                } else {
                    pathName = file.getPath().substring(basePath.length() + 1);
                    InputStream is = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    zos.putNextEntry(new ZipEntry(pathName));
                    while ((length = bis.read(buf)) > 0) {
                        zos.write(buf, 0, length);
                    }
                    is.close();
                }
            }
        } catch (Exception e) {
            //
            e.printStackTrace();
        }

    }

    /**
     * @param inFile
     * @param zos
     * @param dir
     * @throws IOException
     */
    public static void zipFile(File inFile, ZipOutputStream zos, String dir) throws IOException {
        if (inFile.isDirectory()) {
            File[] files = inFile.listFiles();
            for (File file : files)
                zipFile(file, zos, dir + Constances.BACK_SLASH + inFile.getName());
        } else {
            String entryName = null;
            if (!"".equals(dir))
                entryName = dir + Constances.BACK_SLASH + inFile.getName();
            else
                entryName = inFile.getName();
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            InputStream is = new FileInputStream(inFile);
            int len = 0;
            while ((len = is.read()) != -1)
                zos.write(len);
            is.close();
        }
    }

    /**
     * 解压缩zip文件
     * 
     * @param zipfile
     * @param destDir
     * @throws IOException
     */
    public static void unpack(String zipfile, String destDir) {
        destDir = destDir.endsWith("//") ? destDir : destDir + "//";
        byte b[] = new byte[1024];
        int length;

        ZipFile zipFile;
        try {
            zipFile = new ZipFile(new File(zipfile));
            @SuppressWarnings("unchecked")
            Enumeration<ZipEntry> enumeration = (Enumeration<ZipEntry>) zipFile.entries();
            ZipEntry zipEntry = null;

            while (enumeration.hasMoreElements()) {
                zipEntry = (ZipEntry) enumeration.nextElement();
                File loadFile = new File(destDir + zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    // 这段都可以不要，因为每次都貌似从最底层开始遍历的
                    loadFile.mkdirs();
                } else {
                    if (!loadFile.getParentFile().exists()) {
                        loadFile.getParentFile().mkdirs();
                    }

                    @SuppressWarnings("resource")
                    OutputStream outputStream = new FileOutputStream(loadFile);
                    InputStream inputStream = zipFile.getInputStream(zipEntry);

                    while ((length = inputStream.read(b)) > 0) {
                        outputStream.write(b, 0, length);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 此方法通过ZipFile和ZipInputStream共同来解压文件的，通过ZipInputStream可以拿到ZipFile中的每一个
     * ZipEntry对象，然后通过ZipFile的getInputStream(ZipEntry
     * zipEntry)方法拿到对应的每一个ZipEntry的 输入流，从而实现文件的解压
     * 
     * @param zipFile
     * @param zipInputStream
     * @throws IOException
     */
    public static void unpack(ZipFile zipFile, ZipInputStream zipInputStream) throws IOException {
        ZipEntry zipEntry = null;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            String fileName = zipEntry.getName();
            File temp = new File("D:\\unpackTest\\" + fileName);
            if (!temp.getParentFile().exists()) {
                temp.getParentFile().mkdirs();
            }
            OutputStream os = new FileOutputStream(temp);
            // 通过ZipFile的getInputStream方法拿到具体的ZipEntry的输入流
            InputStream is = zipFile.getInputStream(zipEntry);
            int len = 0;
            System.out.println(zipEntry.getName());
            while ((len = is.read()) != -1) {
                os.write(len);
            }
            os.close();
            is.close();
        }
        zipInputStream.close();
    }

    /**
     * 该方法是直接通过ZipFile的entries()方法拿到包含所有的ZipEntry对象的Enumeration对象，
     * 接下来的操作和上面的是一样的
     * 
     * @param zipFile
     * @throws IOException
     */
    public static void unpack(ZipFile zipFile) throws IOException {
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String fileName = zipEntry.getName();
            File temp = new File("D:\\unpackTest2\\" + fileName);
            if (!temp.getParentFile().exists())
                temp.getParentFile().mkdirs();
            OutputStream os = new FileOutputStream(temp);
            // 通过ZipFile的getInputStream方法拿到具体的ZipEntry的输入流
            InputStream is = zipFile.getInputStream(zipEntry);
            int len = 0;
            while ((len = is.read()) != -1) {
                os.write(len);
            }
            os.close();
            is.close();
        }
    }

}

package helpers;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
 
/**
 * This utility extracts files and directories of a standard zip file to
 * a destination directory.
 * @author www.codejava.net
 *
 */
public class UnzipUtility {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
   
    public static void unzip(byte[] zipFile, Path destDirectory) throws IOException {   
        /* Los archivos vienen dentro de una carpeta llama Docs, hay que crear este
        directorio primero o si no ocurre una excepción al tratar de descomprimirlos*/
        File dirDocs = new File(destDirectory + File.separator + "Docs");
        dirDocs.mkdir();
        ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipFile));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (entry.isDirectory()) {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            } else {            
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}
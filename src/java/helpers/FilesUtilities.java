/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author JSolorzanoS
 */
public class FilesUtilities {

    
    public static void deleteRecursive(File path) throws IOException {
        if(path.isDirectory()){
            for(File f: path.listFiles()){
                deleteRecursive(f);
            }
        }
        Files.delete(path.toPath());
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JSolorzanoS
 */
public class SeparateTokenFiles {

    Path basePath, pathTokens, pathLemmas, pathTags;

    public SeparateTokenFiles(Path basePath) {
        this.basePath = basePath;
    }

    public void process() throws IOException {
        Path pathDocs = Paths.get(basePath.toString(), "Docs");
        pathTokens = Paths.get(basePath.toString(), "tokens");
        pathLemmas = Paths.get(basePath.toString(), "lemmas");
        pathTags = Paths.get(basePath.toString(), "tags");
        Files.createDirectory(pathTokens);
        Files.createDirectory(pathLemmas);
        Files.createDirectory(pathTags);
        File[] files = pathDocs.toFile().listFiles();
        Logger.getLogger(SeparateTokenFiles.class.getName()).log(Level.INFO, "Path docs " + pathDocs);
        for (File f : files) {
            separate(f);
        }
    }

    public void separate(File f) throws IOException, FileNotFoundException, UnsupportedEncodingException {
        Path pathFileTokens = Paths.get(pathTokens.toString(), f.getName());
        Path pathFileLemmas = Paths.get(pathLemmas.toString(), f.getName());
        Path pathFileTags = Paths.get(pathTags.toString(), f.getName());

        try (
                PrintWriter outTokens = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathFileTokens.toFile()), "UTF-8"));
                PrintWriter outLemmas = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathFileLemmas.toFile()), "UTF-8"));
                PrintWriter outTags = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathFileTags.toFile()), "UTF-8"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(" ");
                if (values.length == 4) {
                    String token = values[0];
                    String lemma = values[1];
                    String tag = values[2];
                    outTokens.println(token);
                    outLemmas.println(lemma);
                    outTags.println(tag);
                }
            }
        }

    }
}

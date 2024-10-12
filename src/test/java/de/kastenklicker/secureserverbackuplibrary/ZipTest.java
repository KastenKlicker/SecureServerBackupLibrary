package de.kastenklicker.secureserverbackuplibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipTest {

    static File mainDirectory = new File("./src/test/resources/zipTest");

    static File testFile = new File(mainDirectory, "test.zip");

    public ZipTest() {
    }

    @Test
    public void testZip() {

        Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                List.of("*"),
                new ArrayList<>());

        // Pack into zip
        File test = new File(mainDirectory, "test.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100);
    }

    @Test
    public void testZipFolder() {

        Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                List.of("*"),
                new ArrayList<>());

        // Pack into zip
        File test = new File(mainDirectory, "dirInclude");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 200);
    }

    @Test
    public void testZipSubDirFile() {

        Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                List.of("*"),
                new ArrayList<>());

        // Pack into zip
        File test = new File(mainDirectory, "dirInclude/dirInclude.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() < 200);
    }

    @Test
    public void testZipExcludeFiles() {

        Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                List.of("*"),
                List.of("**/*.yml", "*.yml"));

        // Pack into zip
        File test = new File(mainDirectory, "dir");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 200);
    }

    @Test
    public void testZipIncludeAll() {

        Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                List.of("."),
                new ArrayList<>());

        // Pack into zip
        zip.zip(mainDirectory);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 1000);
    }

    @Test
    public void testGetIncludedFiles() {

        // Pack into zip

        Zip zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                List.of("test.txt", "dirInclude"),
                new ArrayList<>());
        
        Set<File> includedFiles = zip.getIncludedFiles();
        
        assertEquals(2, includedFiles.size());
    }

    @Test
    public void testGetIncludedFilesWildcard() {

        // Pack into zip

        Zip zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                List.of("**/*.txt","*.txt"),
                new ArrayList<>());

        Set<File> includedFiles = zip.getIncludedFiles();

        assertEquals(5, includedFiles.size());

        assertTrue(includedFiles.contains(new File(mainDirectory, "test.txt")),
                "test.txt is not included.");
        assertTrue(includedFiles.contains(new File(mainDirectory, "testExclude.txt")), 
                "testExclude.txt is not included.");
        assertTrue(includedFiles.contains(new File(mainDirectory, "dirInclude/dirInclude.txt")), 
                "dirInclude/dirInclude.txt is not included.");
        assertTrue(includedFiles.contains(new File(mainDirectory, "dir/dirInclude.txt")),
                "dir/dirInclude.txt is not included.");
        assertTrue(includedFiles.contains(new File(mainDirectory, "dir/subDir/subDir.txt")),
                "dir/subDir/subDir.txt is not included.");
    }

    @Test
    public void testGetIncludedDir() {

        // Pack into zip

        Zip zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                List.of("dirInclude"),
                new ArrayList<>());

        Set<File> includedFiles = zip.getIncludedFiles();

        assertEquals(1, includedFiles.size());

        assertTrue(includedFiles.contains(new File(mainDirectory, "dirInclude")),
                "dirInclude is not included.");
    }

    @Test
    public void testGetIncludedDifferentDir() {

        // Pack into zip

        Zip zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                List.of("*/dirInclude.txt"),
                new ArrayList<>());

        Set<File> includedFiles = zip.getIncludedFiles();

        assertEquals(2, includedFiles.size());

        assertTrue(includedFiles.contains(new File(mainDirectory, "dirInclude/dirInclude.txt")),
                "dirInclude/dirInclude.txt is not included.");

        assertTrue(includedFiles.contains(new File(mainDirectory, "dir/dirInclude.txt")),
                "dir/dirInclude.txt is not included.");
    }

    @Test
    public void testGetIncludedDirFile() {

        // Pack into zip

        Zip zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                List.of("dirInclude/*.txt"),
                new ArrayList<>());

        Set<File> includedFiles = zip.getIncludedFiles();

        assertEquals(1, includedFiles.size());

        assertTrue(includedFiles.contains(new File(mainDirectory, "dirInclude/dirInclude.txt")),
                "dirInclude/dirInclude.txt is not included.");
    }

    @AfterEach
    public void deleteTestFile() {
        if (!testFile.delete() && !System.getProperty("os.name").toLowerCase().contains("windows"))
            throw new RuntimeException("Could not delete test file: " + testFile.getAbsolutePath());
    }

}

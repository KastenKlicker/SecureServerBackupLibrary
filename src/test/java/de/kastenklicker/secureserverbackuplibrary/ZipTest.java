package de.kastenklicker.secureserverbackuplibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipTest {

    static File mainDirectory = new File("./src/test/resources/zipTest");
    Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                new ArrayList<>());

    static File testFile = new File(mainDirectory, "test.zip");

    public ZipTest() {
    }

    @Test
    public void testZip() {

        // Pack into zip
        File test = new File(mainDirectory, "test.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100);
    }

    @Test
    public void testZipFolder() {

        // Pack into zip
        File test = new File(mainDirectory, "testDir/testDir.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100);
    }

    @Test
    public void testZipExclude() {

        // Pack into zip
        ArrayList<String> excludeFiles = new ArrayList<>();
        excludeFiles.add("testDir");

        zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                excludeFiles);

        File test = new File(mainDirectory, "testDir/testDir.txt");
        zip.zip(test);
        File testKek = new File(mainDirectory, "test.txt");
        zip.zip(testKek);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100 && testFile.length() < 200);
    }

    @AfterEach
    public void deleteTestFile() {
        if (!testFile.delete() && !System.getProperty("os.name").toLowerCase().contains("windows"))
            throw new RuntimeException("Could not delete test file: " + testFile.getAbsolutePath());
    }

}

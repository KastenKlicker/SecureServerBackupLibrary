package de.kastenklicker.secureserverbackuplibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.*;

import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class for zipping the server files.
 */
public class Zip {

    private final ZipOutputStream zipOutputStream;
    private final FileOutputStream fileOutputStream;
    private final Set<File> includedFiles;
    private final Set<File> excludeFiles;
    private final File serverDirectory;

    /**
     * The constructor for the backup zip file.
     * @param backupFile The zip file.
     * @param serverDirectory The directory where the server.jar is located.
     * @param excludeFiles List of files which should be excluded.
     * @throws ZipException Thrown if file is a directory or can't be accessed/created.
     */
    public Zip(File backupFile, File serverDirectory, List<String> includeFiles, List<String> excludeFiles) {
        this.serverDirectory = serverDirectory;
        
        // Get all files which should be included
        this.includedFiles = getFilesByRegex(includeFiles);
        
        // Get all files which shouldn't be included
        this.excludeFiles = getFilesByRegex(excludeFiles);

        // Create Zip OutputStream
        try {
            fileOutputStream = new FileOutputStream(backupFile);

            zipOutputStream = new ZipOutputStream(fileOutputStream);
            zipOutputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    public Set<File> getIncludedFiles() {
        return includedFiles;
    }

    /**
     * Zip files and directories recursive.
     * @param file file to add
     * @throws ZipException Wrapped IOException
     */
    public void zip(File file) throws ZipException {

        try {
            // Check if file should be excluded from the backup
            if (excludeFiles.contains(file))
                return;

            // If file is a directory, add all of its child files, if there are any
            if (file.isDirectory()) {
                Optional<File[]> optionalFiles = Optional.ofNullable(file.listFiles());
                if (optionalFiles.isPresent()) {
                    for (File child : optionalFiles.get()) {
                        zip(child);
                    }
                }
            } else {

                // File is a real file, so add it
                // Add Entry
                ZipEntry zipEntry = new ZipEntry(file.getAbsolutePath()
                        .replace(serverDirectory.getAbsolutePath(), "").substring(1));

                zipOutputStream.putNextEntry(zipEntry);

                // Write file to zip
                byte[] buf = new byte['?'];

                FileInputStream fileInputStream = new FileInputStream(file);
                int len;
                while ((len = fileInputStream.read(buf)) > 0) {
                    zipOutputStream.write(buf, 0, len);
                }

                zipOutputStream.closeEntry();
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    /**
     * Finish up the zip file
     * @throws ZipException OutputStream Exceptions.
     */
    public void finish() {
        try {
            zipOutputStream.flush();
            zipOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    /**
     * Get Files by its regex path.
     * @param filePaths regex paths
     * @return Set of files found matching at least one regex
     */
    private Set<File> getFilesByRegex(List<String> filePaths) {

        Set<PathMatcher> regexMatchers = new HashSet<>();
        
        // Create Path Matchers from regex
        for (String filePath : filePaths) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePath);
            regexMatchers.add(matcher);
        }
        
        return searchFile(this.serverDirectory, new HashSet<>(), regexMatchers);
    }

    /**
     * Find file by matching regex.
     * @param file the file
     * @param foundFiles already found files
     * @param matchers the path matchers
     * @return Found files, including already found files
     */
    private Set<File> searchFile(File file, Set<File> foundFiles, final Set<PathMatcher> matchers) {

        for (File childFile : file.listFiles()) {

            // Check if file matches one Path Matcher
            for (PathMatcher matcher : matchers) {
                if (matcher.matches(serverDirectory.toPath().relativize(childFile.toPath()))) {
                    // A Matching was found, add to foundFiles Set
                    foundFiles.add(childFile);
                    break;
                }
            }

            // If file is directory, check if one of its children matches a Path
            if (childFile.isDirectory()) {                
                searchFile(childFile, foundFiles, matchers);
            }
        }
        
        return foundFiles;
    }
}

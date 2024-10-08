package de.kastenklicker.secureserverbackuplibrary;

import de.kastenklicker.secureserverbackuplibrary.upload.FTPSClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FTPSClientTest {

    private static FixedHostPortGenericContainer<?> ftpsContainer;
    
    private static final File parentDir = new File("./src/test/resources/cert/");
    private static final File certFile = new File(parentDir, "cert.pem");
    private static final File keyFile = new File(parentDir,  "key.pem");

    private static String hostname;
    private static int port;
    private static final String USERNAME = "alpineftp";
    private static final String AUTHENTICATION = "alpineftp"; // password
    private static final String REMOTE_DIRECTORY = "/ftp/alpineftp";
    private static final int PASSIV_PORT = 21000;
    
    @BeforeAll
    public static void BeforeAll() throws Exception {
        
        // Generate Certificate and Key file
        // Check if parent directory exists
        if (!parentDir.exists() && !parentDir.mkdir())
            throw new RuntimeException("Couldn't create certificate directory.");
        CertGenerator.generate(certFile, keyFile);
        
        // Create & start Docker Container
        ftpsContainer = new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
                .withFixedExposedPort(PASSIV_PORT, PASSIV_PORT)
                .withExposedPorts(21)
                .withEnv("MIN_PORT", String.valueOf(PASSIV_PORT))
                .withEnv("MAX_PORT", String.valueOf(PASSIV_PORT))
                .withEnv("TLS_CERT", "/letsencrypt/cert.pem")
                .withEnv("TLS_KEY", "/letsencrypt/key.pem")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(parentDir.getAbsolutePath()),
                        "/letsencrypt"
                )
        ;
        
        ftpsContainer.start();
        
        hostname = ftpsContainer.getHost();
        port = ftpsContainer.getMappedPort(21);
    }

    /**
     * Checks if the upload is working fine
     * @throws Exception FTPS Exceptions
     */
    @Test
    public void testUpload() throws Exception {
        
        final FTPSClient ftpsClient = new FTPSClient(hostname, port, USERNAME,
                AUTHENTICATION, REMOTE_DIRECTORY);
        
        File testFile = new File("./src/test/resources/zipTest/test.txt");
         ftpsClient.upload(testFile);

        // Check if file was transferred correctly
        File testFileUpload = new File("./src/test/resources/testUpload.txt");
        ftpsContainer.copyFileFromContainer(REMOTE_DIRECTORY + "/test.txt", testFileUpload.getPath());
        assertEquals(-1L,
                Files.mismatch(testFile.toPath(), testFileUpload.toPath()),
                "Uploaded and Download files are not the same");
        assertTrue(testFileUpload.delete());
    }

    @AfterAll
    public static void afterAll() {

        // Stop Docker Container
        ftpsContainer.stop();
        
        // Delete certificates
        certFile.delete();
        keyFile.delete();
        parentDir.delete();

    }
}

package de.kastenklicker.secureserverbackuplibrary;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import de.kastenklicker.secureserverbackuplibrary.upload.SFTPClient;
import de.kastenklicker.secureserverbackuplibrary.upload.UploadException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class SFTPClientTest {

    private static GenericContainer<?> sftpContainer;
    
    private static File privateHostKey;
    private static File publicHostKey;
    private static final File TESTFILE = new File("./src/test/resources/zipTest/test.txt");

    private static String hostname;
    private static int port;
    private final String username = "foo";
    private final String authentication = "pass"; // password or path of private key file
    private final int timeout = 20000;
    private final String remoteDirectory = "/upload"; // Must not end with slash
    
    @BeforeAll
    public static void BeforeAll() throws IOException, JSchException {
        
        // Generate Test RSA Keys
        KeyPairGenerator keyPairGenerator = new KeyPairGenerator();
        keyPairGenerator.generate();
        publicHostKey = keyPairGenerator.getPublicKeyFile();
        privateHostKey = keyPairGenerator.getPrivateKeyFile();
        
        // Create & start Docker Container        
        sftpContainer = new GenericContainer<>("atmoz/sftp:alpine")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(privateHostKey.getAbsolutePath()),
                        "/etc/ssh/ssh_host_rsa_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload");
        
        sftpContainer.start();

        hostname = sftpContainer.getHost();
        port = sftpContainer.getMappedPort(22);
    }
    
    @Test
    public void testUpload() throws IOException {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey, timeout, remoteDirectory);
        
        sftpClient.upload(TESTFILE);
        
        // Check if file was transferred correctly
        File testFileUpload = new File("./src/test/resources/testUpload.txt");
        sftpContainer.copyFileFromContainer("/home/foo/upload/test.txt", testFileUpload.getPath());
        assertEquals(-1L,
                Files.mismatch(TESTFILE.toPath(), testFileUpload.toPath()),
                "Uploaded and Download files are not the same");
        assertTrue(testFileUpload.delete());
    }
    
    @Test
    public void testUploadScanHostKey() throws IOException {
        
        if (!publicHostKey.delete() && publicHostKey.exists())
            throw new RuntimeException("Couldn't run test, because publicHostKey file couldn't be deleted");
        
        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey, timeout, remoteDirectory);
        
        sftpClient.upload(TESTFILE);

        // Check if file was transferred correctly
        File testFileUpload = new File("./src/test/resources/testUpload.txt");
        sftpContainer.copyFileFromContainer("/home/foo/upload/test.txt", testFileUpload.getPath());
        assertEquals(-1L,
                Files.mismatch(TESTFILE.toPath(), testFileUpload.toPath()),
                "Uploaded and Download files are not the same");
        assertTrue(testFileUpload.delete());
    }

    @Test
    public void testUploadWrongDirectory() {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey, timeout,  remoteDirectory + "/sus/kek");

        Exception exception = assertThrows(UploadException.class, () ->
                sftpClient.upload(TESTFILE)
        );

        assertInstanceOf(SftpException.class, exception.getCause());
    }
    
    @Test
    public void testUploadWrongHost() {
        final SFTPClient sftpClient = new SFTPClient("hostname", port, username,
                authentication, publicHostKey, timeout, remoteDirectory);

        Exception exception = assertThrows(UploadException.class, () ->
                sftpClient.upload(TESTFILE)
        );

        assertInstanceOf(UnknownHostException.class, exception.getCause().getCause());
    }
    
    @Test
    public void testUploadNoLocalFile() {
        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey, timeout, remoteDirectory);

        Exception exception = assertThrows(UploadException.class, () ->
                sftpClient.upload(new File("kek"))
        );

        assertInstanceOf(FileNotFoundException.class, exception.getCause().getCause());
    }
    
    @AfterAll
    public static void afterAll() {
        
        // Stop Docker Container
        sftpContainer.stop();
        
        // Remove HostKeyFiles
        publicHostKey.delete();
        privateHostKey.delete();
        
    }

}

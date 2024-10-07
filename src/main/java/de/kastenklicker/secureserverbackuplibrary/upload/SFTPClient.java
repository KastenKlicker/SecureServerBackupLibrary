package de.kastenklicker.secureserverbackuplibrary.upload;

import com.jcraft.jsch.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Properties;

/**
 * Class for uploading backup with SFTP.
 */
public class SFTPClient extends UploadClient{
    
    private final File publicHostKeyFile;
    private final int timeout;

    /**
     * Creates the SFTP Client.
     * @param hostname Hostname/Domain/IP Address of remote server.
     * @param port Port of remote server.
     * @param username Username of remote user.
     * @param authentication Authentication either password or file path to unlocked private RSA key.
     * @param publicHostKeyFile Public host key or knownHosts file.
     *                          If file doesn't exist, the key will be received from the server and saved to the file.
     * @param timeout Session timeout in milliseconds.
     * @param remoteDirectory The directory of the remote server.
     */
    public SFTPClient(String hostname, int port, String username, String authentication,
                      File publicHostKeyFile, int timeout, String remoteDirectory) {
        super(hostname, port, username, authentication, remoteDirectory);
        
        this.publicHostKeyFile = publicHostKeyFile;
        this.timeout = timeout;
    }

    /**
     * Method to upload file to sftp server
     * @param backupFile file to upload
     * @throws JSchException Some exceptions
     * @throws SftpException Some exceptions
     */
    @Override
    public void upload(File backupFile)
            throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        
        // Scan for Host Key if file doesn't exist
        if (!publicHostKeyFile.exists()) {

            // Get host key
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            Session scanSession = jsch.getSession(username, hostname, port);
            scanSession.setConfig(config);
            try {
                scanSession.connect(timeout);
            } catch (JSchException e) {
                if (!e.getMessage().contains("Auth fail"))
                    throw e;
            }
            HostKey hostkey = scanSession.getHostKey();

            // Write host key to file
            BufferedWriter writer = new BufferedWriter(new FileWriter(publicHostKeyFile));
            writer.write(hostkey.getType() + " " + hostkey.getKey() + " " + hostkey.getComment());
            writer.close();
        }
        
        // Check if the given String is the knownHosts file or a HostKey
        String hostKeyFileName = publicHostKeyFile.getName();
        if (hostKeyFileName.length() >= 4 &&    // File ist a public HostKey
                hostKeyFileName.substring(hostKeyFileName.length() - 4).equalsIgnoreCase(".pub")) {
            // Add public hostKey            
            String keyString = Files.readString(publicHostKeyFile.toPath()).trim();
            
            // Get just the Base64 part
            String[] hostKeyParts = keyString.split(" ");
            
            if (hostKeyParts.length != 3)
                throw new RuntimeException("Invalid hostKey format");
            
            keyString = hostKeyParts[1];
            
            // Decode the base64 string
            byte[] key = Base64.getDecoder().decode(keyString);
            HostKey hostKey = new HostKey(hostname, key);
            jsch.getHostKeyRepository().add(hostKey, null);
        }    
        else {  // File is a knownHosts file
            jsch.setKnownHosts(publicHostKeyFile.getPath());
        }
            
        
        Session session = jsch.getSession(username, hostname, port);
        
        // If string is path, then use key authentication, else use password authentication
        if (new File(authentication).exists()) {
            jsch.addIdentity(authentication);
        } else {
            session.setPassword(authentication);
        }

        session.connect(timeout);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");

        // Upload
        channelSftp.connect(timeout);
        channelSftp.put(backupFile.getPath(), remoteDirectory);

        // Disconnect
        channelSftp.exit();
        session.disconnect();
    }

}

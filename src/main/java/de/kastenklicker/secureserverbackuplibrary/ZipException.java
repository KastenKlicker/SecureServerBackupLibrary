package de.kastenklicker.secureserverbackuplibrary;

public class ZipException extends RuntimeException{
    public ZipException(Throwable cause) {
        super("SecureServerBackupLibrary failed to zip the backup.",  cause);
    }
}

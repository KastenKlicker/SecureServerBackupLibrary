package de.kastenklicker.secureserverbackuplibrary;

/**
 * Thrown if a zip related is caught.
 */
public class ZipException extends RuntimeException{
    /**
     * Throw unchecked zip exception.
     * @param cause Zip related exceptions, only IOException
     */
    public ZipException(Throwable cause) {
        super("SecureServerBackupLibrary failed to zip the backup.",  cause);
    }
}

package de.kastenklicker.secureserverbackuplibrary.upload;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class GoogleDriveClient extends UploadClient{
    
    /**
     * Constructor of UploadClient class.
     *
     * @param authentication Authentication either password or file path to unlocked private RSA key.
     */
    public GoogleDriveClient(String authentication) {
        super("www.googleapis.com", -1, "", authentication, "/upload/drive/v3/files");
    }

    @Override
    protected void internalUpload(File file) throws Exception {
        
        if (file.length() > 5*1000000000L)
            throw new RuntimeException(file.getName() + " files greater than 5GB, can't be uploaded to Google Drive");
        
        // Request resumable Upload
        //https://developers.google.com/drive/api/guides/manage-uploads#resumable
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https", hostname, remoteDirectory,"uploadType=resumable", null))
                .header("Authorization", "Bearer " + authentication)
                .header("X-Upload-Content-Length", String.valueOf(file.length()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("{ \"name\": \""+ file.getName() + "\" }"))
                .build();
        
        // Send and get Response
        HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        
        int statusCode = response.statusCode();
        String responseBody = response.body();
        
        // Check if response is positiv
        if (statusCode != 200)
            throw new GoogleDriveException("Response returned status code " + statusCode + " with body: \n" + responseBody);

        Optional<String> optionalLocation = response.headers().firstValue("location");
        if (optionalLocation.isEmpty())
            throw new GoogleDriveException("Response is missing Location header field.");
        
        // Create file upload request
        URI uri = new URI(optionalLocation.get());
        
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(uri)
                .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                .build();
        
        HttpResponse<String> uploadResponse;
        
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            uploadResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            uploadResponse = resumeInterruptedUpload(uri, "*", file.length(), 0);
        }
        
        /*
        Evaluating response's status code
         */
        int uploadStatusCode = uploadResponse.statusCode();
        String uploadResponseBody = uploadResponse.body();
        
        if (uploadStatusCode == 503) // Service Unavailable
            uploadResponse = resumeInterruptedUpload(uri, "*", file.length(), 0);
        
        else if (uploadStatusCode == 308) { // Resume Incomplete
            Optional<String> optionalHeader = uploadResponse.headers().firstValue("Range");
            
            if (optionalHeader.isPresent()) { // if response has no Range header no bytes where received
                String bytesSent = optionalHeader.get().split("-")[1];
                
                // Increment bytesSent by one, so we get the next byte we should send
                int nextByte = Integer.parseInt(bytesSent) + 1;
                
                uploadResponse = resumeInterruptedUpload(uri, String.valueOf(nextByte), file.length(), 0);
                
            } else // no range header, no bytes where received
                uploadResponse = resumeInterruptedUpload(uri, "*", file.length(), 0);
        }
        else if (uploadStatusCode != 200 && uploadStatusCode != 201) { // if Success status code where returned, smth is wrong
                throw new GoogleDriveException("Response returned status code " + uploadResponse + " with body: \n" + uploadResponseBody);
        }

        uploadResponseBody = uploadResponse.body();
        LOGGER.debug(uploadResponseBody);
        
    }

    /**
     * Recursively retrying to upload the file
     * @param resumableSessionURI URI to upload file to
     * @param resumeByte the byte we will need to send next, * if not known
     * @param fileSize the size of the file we want to send
     * @param numberOfRetries the number of retries
     * @return the HTTPResponse
     * @throws IOException Sending or receiving related exception
     * @throws InterruptedException Thread related exception
     */
    private HttpResponse<String> resumeInterruptedUpload(URI resumableSessionURI,
                                                         String resumeByte,
                                                         long fileSize, int numberOfRetries)
            throws IOException, InterruptedException {
        
        // Resume Request
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(resumableSessionURI)
                .header("Content-Range", resumeByte + "/" + fileSize) // interrupt at unknown byte from file size in bytes
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        
        // Try to resume upload
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            return httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (numberOfRetries < 20) { // If we have to many retries stop retrying
                return resumeInterruptedUpload(resumableSessionURI, resumeByte, fileSize, ++numberOfRetries);
            } else
                throw e;
        }
    }
}

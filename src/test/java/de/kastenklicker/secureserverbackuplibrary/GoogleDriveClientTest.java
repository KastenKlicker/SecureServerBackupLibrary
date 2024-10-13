package de.kastenklicker.secureserverbackuplibrary;

import de.kastenklicker.secureserverbackuplibrary.upload.GoogleDriveClient;
import org.junit.jupiter.api.Test;

import java.io.File;

public class GoogleDriveClientTest {
    @Test
    public void upload() {
        GoogleDriveClient httpsClient = new GoogleDriveClient(
            "ya29.a0AcM612xf0FFohzSQiqndNlGRMYzdd-wEziou6ntm7LPK64Qtag_V-3ooyHsZMc8w4Hk7N-aik3IE4JXJOZfmal2tGN-Kwg25e4Bzj9h2p_dGZ_dtuLbqlOSQX2W_Vl-HJG-FaTV-CuQHp_CwyRhH3SZTST_dzxbdSQaCgYKAQ4SARMSFQHGX2MiWkqBl0QJ-FRDelWcrHXYFA0169"
        );
        
        httpsClient.upload(new File("src/test/resources/zipTest/test.txt"));
    }
}

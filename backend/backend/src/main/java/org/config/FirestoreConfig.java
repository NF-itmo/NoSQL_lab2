package org.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {
    @Bean(destroyMethod = "close")
    public static Firestore initialize(
            @Value("${firestore.account-file}") String accountFile
    ) throws IOException {

        FileInputStream serviceAccount = new FileInputStream(accountFile);

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setCredentials(
                        GoogleCredentials.fromStream(serviceAccount)
                )
                .build();

        return options.getService();
    }
}

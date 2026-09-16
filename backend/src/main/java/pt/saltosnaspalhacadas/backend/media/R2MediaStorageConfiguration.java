package pt.saltosnaspalhacadas.backend.media;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.media.storage-provider", havingValue = "r2")
class R2MediaStorageConfiguration {

    @Bean
    @ConditionalOnMissingBean(S3Client.class)
    S3Client r2S3Client(
            @Value("${app.media.r2.endpoint}") String endpoint,
            @Value("${app.media.r2.access-key-id}") String accessKeyId,
            @Value("${app.media.r2.secret-access-key}") String secretAccessKey,
            @Value("${app.media.r2.region:auto}") String region) {
        String endpointValue = required(endpoint, "R2_ENDPOINT é obrigatório quando MEDIA_STORAGE_PROVIDER=r2");
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                required(accessKeyId, "R2_ACCESS_KEY_ID é obrigatório quando MEDIA_STORAGE_PROVIDER=r2"),
                required(secretAccessKey, "R2_SECRET_ACCESS_KEY é obrigatório quando MEDIA_STORAGE_PROVIDER=r2"));

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpointValue))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region == null || region.isBlank() ? "auto" : region.trim()))
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }
}

package pt.saltosnaspalhacadas.backend.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import software.amazon.awssdk.services.s3.S3Client;

class MediaStorageContextTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MediaStorageTestConfiguration.class)
            .withPropertyValues(
                    "app.media.local-directory=build/test-uploads",
                    "app.media.private-local-directory=build/test-uploads-private");

    @Test
    void r2ProviderCreatesOnlyR2MediaStorageWithMockS3Client() {
        S3Client s3 = mock(S3Client.class);

        contextRunner
                .withBean(S3Client.class, () -> s3)
                .withPropertyValues(
                        "app.media.storage-provider=r2",
                        "app.media.r2.endpoint=https://account.eu.r2.cloudflarestorage.com",
                        "app.media.r2.access-key-id=access-key-id",
                        "app.media.r2.secret-access-key=secret-access-key",
                        "app.media.r2.public-bucket=saltos-public",
                        "app.media.r2.private-bucket=saltos-private",
                        "app.media.r2.region=auto")
                .run(context -> {
                    assertThat(context).hasSingleBean(MediaStorage.class);
                    assertThat(context).hasSingleBean(R2MediaStorage.class);
                    assertThat(context).doesNotHaveBean(LocalMediaStorage.class);
                    assertThat(context).hasSingleBean(S3Client.class);
                    assertThat(context.getBean(MediaStorage.class)).isInstanceOf(R2MediaStorage.class);
                    verifyNoInteractions(s3);
                });
    }

    @Test
    void localProviderCreatesOnlyLocalMediaStorage() {
        contextRunner
                .withPropertyValues("app.media.storage-provider=local")
                .run(context -> {
                    assertThat(context).hasSingleBean(MediaStorage.class);
                    assertThat(context).hasSingleBean(LocalMediaStorage.class);
                    assertThat(context).doesNotHaveBean(R2MediaStorage.class);
                    assertThat(context).doesNotHaveBean(S3Client.class);
                    assertThat(context.getBean(MediaStorage.class)).isInstanceOf(LocalMediaStorage.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            MediaFileValidator.class,
            LocalMediaStorage.class,
            R2MediaStorage.class,
            R2MediaStorageConfiguration.class
    })
    static class MediaStorageTestConfiguration {
    }
}

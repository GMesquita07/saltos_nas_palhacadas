package pt.saltosnaspalhacadas.backend.clientcontent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import pt.saltosnaspalhacadas.backend.auth.JwtService;
import pt.saltosnaspalhacadas.backend.media.ManagedMedia;
import pt.saltosnaspalhacadas.backend.media.ManagedMediaRepository;
import pt.saltosnaspalhacadas.backend.media.MediaStorage;
import pt.saltosnaspalhacadas.backend.media.StoredMedia;
import pt.saltosnaspalhacadas.backend.user.AppUser;
import pt.saltosnaspalhacadas.backend.user.AppUserRepository;
import pt.saltosnaspalhacadas.backend.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientContentMediaControllerStorageMockTests {
    private static final String STORAGE_KEY = "123e4567-e89b-12d3-a456-426614174000.mp4";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AppUserRepository users;
    @Autowired private ManagedMediaRepository managedMedia;

    @MockitoBean
    private MediaStorage storage;

    @Test
    void authenticatedCustomerCanUploadClientContentMediaThroughStorageBean() throws Exception {
        when(storage.storePrivate(any(MultipartFile.class))).thenReturn(new StoredMedia(STORAGE_KEY, "video/mp4"));
        AppUser customer = users.save(new AppUser(
                "storage-mock-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test",
                passwords.encode("change-me-now"),
                UserRole.CUSTOMER));
        String token = jwtService.createToken(customer);

        try {
            MockMultipartFile file = new MockMultipartFile("file", "evento.mp4", "video/mp4", mp4Header());

            mockMvc.perform(multipart("/api/v1/client-posts/media")
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.contentType").value("video/mp4"))
                    .andExpect(jsonPath("$.url", containsString("/api/v1/private-media/" + STORAGE_KEY)));

            ArgumentCaptor<MultipartFile> uploadedFile = ArgumentCaptor.forClass(MultipartFile.class);
            verify(storage).storePrivate(uploadedFile.capture());
            assertThat(uploadedFile.getValue().getOriginalFilename()).isEqualTo("evento.mp4");
        } finally {
            for (ManagedMedia media : managedMedia.findAllByOwnerId(customer.getId())) {
                managedMedia.delete(media);
            }
            users.deleteById(customer.getId());
        }
    }

    private static byte[] mp4Header() {
        return new byte[] {0, 0, 0, 24, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6f, 0x6d, 0, 0, 0, 1};
    }
}

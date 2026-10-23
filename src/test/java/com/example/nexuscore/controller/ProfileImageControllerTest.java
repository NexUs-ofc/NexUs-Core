package com.example.nexuscore.controller;

import com.example.nexuscore.dto.profile.ProfileImageResponse;
import com.example.nexuscore.exception.GlobalExceptionHandler;
import com.example.nexuscore.image.ImageStorageException;
import com.example.nexuscore.service.CompanyService;
import com.example.nexuscore.service.PaymentService;
import com.example.nexuscore.service.ProfileImageService;
import com.example.nexuscore.service.ProfileService;
import com.example.nexuscore.service.StoreService;
import com.example.nexuscore.util.CurrentProfileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileImageControllerTest {

    private final ProfileImageService images = mock(ProfileImageService.class);
    private final CurrentProfileResolver currentProfile = mock(CurrentProfileResolver.class);
    private final MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(currentProfile.profileId()).thenReturn(12L);
        mvc = MockMvcBuilders.standaloneSetup(
                new ProfileController(mock(ProfileService.class), currentProfile, images),
                new CompanyController(mock(CompanyService.class), mock(StoreService.class),
                        mock(PaymentService.class), currentProfile, images))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void acceptsPutMultipartAndUsesAuthenticatedProfile() throws Exception {
        when(images.uploadOwn(eq(12), any())).thenReturn(new ProfileImageResponse(12, "https://example.com/new.png"));

        mvc.perform(multipart(HttpMethod.PUT, "/api/profile/image").file(file).param("profileId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(12))
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/new.png"));

        verify(images).uploadOwn(eq(12), any());
    }

    @Test
    void removesOwnImage() throws Exception {
        when(images.removeOwn(12)).thenReturn(new ProfileImageResponse(12, null));

        mvc.perform(delete("/api/profile/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(nullValue()));

        verify(images).removeOwn(12);
    }

    @Test
    void acceptsStoreImageUploadAtExistingCompanyRoute() throws Exception {
        when(images.uploadStore(eq(12), eq(77), any()))
                .thenReturn(new ProfileImageResponse(20, "https://example.com/store.png"));

        mvc.perform(multipart(HttpMethod.PUT, "/api/companies/me/stores/77/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(20));

        verify(images).uploadStore(eq(12), eq(77), any());
    }

    @Test
    void removesOwnedStoreImage() throws Exception {
        when(images.removeStore(12, 77)).thenReturn(new ProfileImageResponse(20, null));

        mvc.perform(delete("/api/companies/me/stores/77/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(nullValue()));
    }

    @Test
    void rejectsMissingFile() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/api/profile/image"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(images);
    }

    @Test
    void reportsProviderFailureAsBadGateway() throws Exception {
        when(images.uploadOwn(eq(12), any()))
                .thenThrow(new ImageStorageException(HttpStatus.BAD_GATEWAY, "Falha no Cloudinary"));

        mvc.perform(multipart(HttpMethod.PUT, "/api/profile/image").file(file))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Falha no Cloudinary"));
    }

    @Test
    void reportsOversizedUploadAs413() throws Exception {
        when(images.uploadOwn(eq(12), any())).thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        mvc.perform(multipart(HttpMethod.PUT, "/api/profile/image").file(file))
                .andExpect(status().isPayloadTooLarge());
    }
}

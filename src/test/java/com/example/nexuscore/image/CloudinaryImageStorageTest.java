package com.example.nexuscore.image;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CloudinaryImageStorageTest {

    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private final CloudinaryProperties credentials = new CloudinaryProperties("demo", "test-key", "test-secret");
    private final CloudinaryImageStorage storage = new CloudinaryImageStorage(credentials, cloudinary);

    @Test
    void uploadsWithRestrictedFormatsAndReturnsHttpsUrl() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/test.png", "public_id", "nexus/profiles/12/abc"));
        byte[] bytes = {1, 2};

        assertEquals("https://res.cloudinary.com/demo/test.png", storage.upload(bytes, "nexus/profiles/12/abc"));

        verify(uploader).upload(eq(bytes), argThat(options ->
                "image".equals(options.get("resource_type")) && Boolean.FALSE.equals(options.get("overwrite"))
                        && List.of("jpg", "jpeg", "png", "webp").equals(options.get("allowed_formats"))));
    }

    @Test
    void convertsProviderFailureWithoutLeakingCredentials() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("test-secret"));

        var exception = assertThrows(ImageStorageException.class, () -> storage.upload(new byte[]{1}, "abc"));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertFalse(exception.getMessage().contains("test-secret"));
        assertFalse(credentials.toString().contains("test-secret"));
    }

    @Test
    void rejectsInsecureOrMalformedProviderResponse() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "http://example.com/image"));

        assertThrows(ImageStorageException.class, () -> storage.upload(new byte[]{1}, "abc"));
    }

    @Test
    void rejectsUploadWithoutCredentials() {
        var unconfigured = new CloudinaryImageStorage(new CloudinaryProperties("", "", ""), cloudinary);

        var exception = assertThrows(ImageStorageException.class, () -> unconfigured.upload(new byte[]{1}, "abc"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        verifyNoInteractions(cloudinary);
    }
}

package com.example.nexuscore.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileImageValidatorTest {

    private final ProfileImageValidator validator = new ProfileImageValidator();

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    void detectsActualContentRegardlessOfFilenameAndDeclaredMimeType(String format) throws Exception {
        byte[] bytes = image(format);
        MockMultipartFile file = new MockMultipartFile("file", "photo.txt", "text/plain", bytes);

        assertArrayEquals(bytes, validator.validate(file));
    }

    @Test
    void acceptsWebp() {
        byte[] bytes = Base64.getDecoder().decode(
                "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

        assertArrayEquals(bytes, validator.validate(new MockMultipartFile("file", bytes)));
    }

    @Test
    void rejectsTextDisguisedAsImage() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsEmptyFile() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new MockMultipartFile("file", new byte[0])));
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[(int) ProfileImageValidator.MAX_BYTES + 1]);

        assertThrows(MaxUploadSizeExceededException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsUnsupportedImageFormat() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new MockMultipartFile("file", image("gif"))));
    }

    @Test
    void rejectsCorruptPng() {
        byte[] bytes = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new MockMultipartFile("file", bytes)));
    }

    @Test
    void rejectsHugeDimensionsBeforeDecoding() throws Exception {
        byte[] bytes = image("png");
        ByteBuffer.wrap(bytes).putInt(16, 100000).putInt(20, 100000);

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(new MockMultipartFile("file", bytes)));
    }

    private byte[] image(String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }
}

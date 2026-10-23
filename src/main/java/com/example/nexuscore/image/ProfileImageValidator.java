package com.example.nexuscore.image;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
public class ProfileImageValidator {

    public static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private static final Set<String> FORMATS = Set.of("jpeg", "jpg", "png", "webp");

    public byte[] validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie uma imagem no campo file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_BYTES);
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) {
                throw new MaxUploadSizeExceededException(MAX_BYTES);
            }
            validateContent(bytes);
            return bytes;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Imagem invalida ou corrompida");
        }
    }

    private void validateContent(byte[] bytes) throws IOException {
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Formato invalido: envie JPEG, PNG ou WebP");
            }
            ImageReader reader = readers.next();
            try {
                if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Formato invalido: envie JPEG, PNG ou WebP");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("A imagem deve ter no maximo 16 megapixels");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("Imagem invalida ou corrompida");
                }
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Imagem invalida ou corrompida");
        }
    }
}

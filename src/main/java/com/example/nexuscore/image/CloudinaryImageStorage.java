package com.example.nexuscore.image;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CloudinaryImageStorage implements ImageStorage {

    private final CloudinaryProperties properties;
    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryImageStorage(CloudinaryProperties properties) {
        this(properties, new Cloudinary(Map.of(
                "cloud_name", value(properties.cloudName()), "api_key", value(properties.apiKey()),
                "api_secret", value(properties.apiSecret()), "secure", true)));
    }

    CloudinaryImageStorage(CloudinaryProperties properties, Cloudinary cloudinary) {
        this.properties = properties;
        this.cloudinary = cloudinary;
    }

    @Override
    public boolean configured() {
        return properties.configured();
    }

    @Override
    public String upload(byte[] bytes, String publicId) {
        requireConfiguration();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(bytes, Map.of(
                    "public_id", publicId, "resource_type", "image", "overwrite", false,
                    "allowed_formats", List.of("jpg", "jpeg", "png", "webp"), "timeout", 60000));
            Object url = result.get("secure_url");
            if (!publicId.equals(result.get("public_id")) || !(url instanceof String secureUrl)
                    || !secureUrl.startsWith("https://") || secureUrl.length() > 500) {
                throw failure();
            }
            return secureUrl;
        } catch (IOException | RuntimeException exception) {
            throw failure();
        }
    }

    private void requireConfiguration() {
        if (!configured()) {
            throw new ImageStorageException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Upload de imagens indisponivel: configure as credenciais do Cloudinary no Core");
        }
    }

    private ImageStorageException failure() {
        return new ImageStorageException(HttpStatus.BAD_GATEWAY, "Falha na comunicacao com o Cloudinary");
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}

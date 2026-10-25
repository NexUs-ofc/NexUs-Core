package com.example.nexuscore.image;

public interface ImageStorage {

    boolean configured();

    String upload(byte[] bytes, String publicId);
}

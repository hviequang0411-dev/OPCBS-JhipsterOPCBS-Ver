package com.ocbs.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Utility helper for loading, parsing, and deriving RSA keys from PEM strings, files, or resources.
 */
public final class RsaKeyHelper {

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    private RsaKeyHelper() {}

    public static RSAPrivateKey parsePrivateKey(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return null;
        }
        String content = resolveContent(keyOrPath.trim());
        boolean isPkcs1 = content.contains("RSA PRIVATE KEY");
        String cleaned = cleanKeyContent(content, "PRIVATE KEY");
        try {
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            if (isPkcs1) {
                decoded = convertPkcs1ToPkcs8(decoded);
            }
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            if (!isPkcs1) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(cleaned);
                    byte[] pkcs8Bytes = convertPkcs1ToPkcs8(decoded);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    return (RSAPrivateKey) kf.generatePrivate(spec);
                } catch (Exception ignored) {}
            }
            throw new IllegalArgumentException("Failed to parse RSA Private Key: " + e.getMessage(), e);
        }
    }

    public static RSAPublicKey parsePublicKey(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return null;
        }
        String content = resolveContent(keyOrPath.trim());
        String cleaned = cleanKeyContent(content, "PUBLIC KEY");
        try {
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse RSA Public Key: " + e.getMessage(), e);
        }
    }

    public static RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        if (privateKey instanceof RSAPrivateCrtKey crtKey) {
            try {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return (RSAPublicKey) kf.generatePublic(spec);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to derive RSA Public Key from Private Key: " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException("Private key does not contain CRT public exponent info to derive public key");
    }

    private static String resolveContent(String input) {
        if (input.contains("BEGIN") || (input.length() > 200 && !input.contains("\n") && !input.contains(" ") && !input.contains("/") && !input.contains("\\"))) {
            return input;
        }

        try {
            Resource resource = RESOURCE_LOADER.getResource(input);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ignored) {}

        try {
            Path path = Paths.get(input);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        return input;
    }

    private static String cleanKeyContent(String content, String type) {
        return content
            .replace("-----BEGIN " + type + "-----", "")
            .replace("-----END " + type + "-----", "")
            .replace("-----BEGIN RSA " + type + "-----", "")
            .replace("-----END RSA " + type + "-----", "")
            .replaceAll("\\s+", "");
    }

    private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] pkcs8Header;

        if (totalLength <= 0x7F) {
            pkcs8Header = new byte[] {
                0x30, (byte) totalLength,
                0x02, 0x01, 0x00,
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, (byte) pkcs1Length
            };
        } else if (totalLength <= 0xFF) {
            pkcs8Header = new byte[] {
                0x30, (byte) 0x81, (byte) totalLength,
                0x02, 0x01, 0x00,
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, (byte) 0x81, (byte) pkcs1Length
            };
        } else {
            pkcs8Header = new byte[] {
                0x30, (byte) 0x82, (byte) (totalLength >> 8), (byte) (totalLength & 0xFF),
                0x02, 0x01, 0x00,
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, (byte) 0x82, (byte) (pkcs1Length >> 8), (byte) (pkcs1Length & 0xFF)
            };
        }

        byte[] pkcs8Bytes = new byte[pkcs8Header.length + pkcs1Bytes.length];
        System.arraycopy(pkcs8Header, 0, pkcs8Bytes, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pkcs8Header.length, pkcs1Bytes.length);
        return pkcs8Bytes;
    }
}

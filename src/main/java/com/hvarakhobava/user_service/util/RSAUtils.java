package com.hvarakhobava.user_service.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author Hanna Varakhobava
 */
public class RSAUtils {

    private static final String PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
    private static final String RSA = "RSA";

    public static PrivateKey loadPrivateKey(String pem) throws InvalidKeySpecException {
        String stripped = pem
                .replace(PRIVATE_KEY_HEADER, "")
                .replace(PRIVATE_KEY_FOOTER, "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(stripped);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        try {
            return KeyFactory.getInstance(RSA).generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey loadPublicKey(String pem) throws InvalidKeySpecException {
        String stripped = pem
                .replace(PUBLIC_KEY_HEADER, "")
                .replace(PUBLIC_KEY_FOOTER, "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(stripped);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        try {
            return KeyFactory.getInstance(RSA).generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
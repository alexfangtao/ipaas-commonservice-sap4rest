package org.apache.camel.sapagent4rest.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;


@Slf4j
@Component
public class RsaUtil {

    private final PrivateKey privateKey;

    public RsaUtil(@Value("${camel.custom.rsa.private}") String rsaPrivateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(rsaPrivateKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("init RSA private key failed", e);
        }
    }

    public String decryptStr(String str) {
        if (str == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(str));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("RSA decrypt failed", e);
        }
    }
}

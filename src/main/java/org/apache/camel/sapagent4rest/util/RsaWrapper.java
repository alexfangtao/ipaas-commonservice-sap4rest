package org.apache.camel.sapagent4rest.util;

import cn.hutool.crypto.asymmetric.RSA;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RsaWrapper extends RSA {
    public RsaWrapper() {
    }

    public RsaWrapper(String rsaAlgorithm) {
        super(rsaAlgorithm);
    }

    public RsaWrapper(String privateKeyStr, String publicKeyStr) {
        super(privateKeyStr, publicKeyStr);
    }

    public RsaWrapper(String rsaAlgorithm, String privateKeyStr, String publicKeyStr) {
        super(rsaAlgorithm, privateKeyStr, publicKeyStr);
    }

    public RsaWrapper(byte[] privateKey, byte[] publicKey) {
        super(privateKey, publicKey);
    }

    public RsaWrapper(BigInteger modulus, BigInteger privateExponent, BigInteger publicExponent) {
        super(modulus, privateExponent, publicExponent);
    }

    public RsaWrapper(PrivateKey privateKey, PublicKey publicKey) {
        super(privateKey, publicKey);
    }

    public RsaWrapper(String rsaAlgorithm, PrivateKey privateKey, PublicKey publicKey) {
        super(rsaAlgorithm, privateKey, publicKey);
    }
}

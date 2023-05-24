package io.naraway.prologue.security.auth.jwt;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import io.naraway.accent.util.uuid.TinyUUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwkTools {
    //
    public static RSAKey rsaKey() {
        //
        KeyPair keyPair = genRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public static RSAKey rsaKey(String publicKeyText, String privateKeyText, String key) {
        //
        KeyPair keyPair = asRsaKey(publicKeyText, privateKeyText);
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(key)
                .build();
    }

    public static ECKey ecKey() {
        //
        KeyPair keyPair = genEcKey();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        Curve curve = Curve.forECParameterSpec(publicKey.getParams());

        return new ECKey.Builder(curve, publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public static ECKey ecKey(String publicKeyText, String privateKeyText, String key) {
        //
        KeyPair keyPair = asEcKey(publicKeyText, privateKeyText);
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        Curve curve = Curve.forECParameterSpec(publicKey.getParams());

        return new ECKey.Builder(curve, publicKey)
                .privateKey(privateKey)
                .keyID(key)
                .build();
    }

    private static KeyPair genRsaKey() {
        //
        KeyPair keyPair;

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return keyPair;
    }

    private static KeyPair asRsaKey(String publicKeyText, String privateKeyText) {
        //
        KeyPair keyPair;

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] publicKeyEncoded = decode(publicKeyText);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            byte[] privateKeyEncoded = decode(privateKeyText);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            keyPair = new KeyPair(publicKey, privateKey);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return keyPair;
    }

    private static KeyPair genEcKey() {
        //
        KeyPair keyPair;

        EllipticCurve ellipticCurve = new EllipticCurve(
                new ECFieldFp(new BigInteger(
                        "115792089210356248762697446949407573530086143415290314195533631308867097853951")),
                new BigInteger(
                        "115792089210356248762697446949407573530086143415290314195533631308867097853948"),
                new BigInteger(
                        "41058363725152142129326129780047268409114441015993725554835256314039467401291"));

        ECPoint ecPoint = new ECPoint(new BigInteger(
                "48439561293906451759052585252797914202762949526041747995844080717082404635286"),
                new BigInteger(
                        "36134250956749795798585127919587881956611106672985015071877198253568414405109"));

        ECParameterSpec ecParameterSpec = new ECParameterSpec(ellipticCurve, ecPoint,
                new BigInteger(
                        "115792089210356248762697446949407573529996955224135760342422259061068512044369"), 1);

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(ecParameterSpec);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return keyPair;
    }

    private static KeyPair asEcKey(String publicKeyText, String privateKeyText) {
        //
        KeyPair keyPair;

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");

            byte[] publicKeyEncoded = decode(publicKeyText);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            byte[] privateKeyEncoded = decode(privateKeyText);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            keyPair = new KeyPair(publicKey, privateKey);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        return keyPair;
    }

    @SuppressWarnings({"java:S5361", "java:S5996"})
    private static String strip(String keyText) {
        //
        Pattern pattern = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
        String striped = pattern.matcher(keyText).replaceFirst("$1");

        return striped.replaceAll("\\n", "");
    }

    private static byte[] decode(String keyText) {
        //
        return Base64.getDecoder().decode(strip(keyText));
    }

    private static String encode(Key key) {
        //
        return new String(Base64.getEncoder().encode(key.getEncoded()));
    }

    private static String toPublicKeyFormat(String key) {
        //
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN RSA PUBLIC KEY-----\n");
        String[] chunks = key.split("(?<=\\G.{" + 64 + "})");
        for (String chunk : chunks) {
            builder.append(chunk).append("\n");
        }
        builder.append("-----END RSA PUBLIC KEY-----\n");

        return builder.toString();
    }

    private static String toPrivateKeyFormat(String key) {
        //
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN RSA PRIVATE KEY-----\n");
        String[] chunks = key.split("(?<=\\G.{" + 64 + "})");
        for (String chunk : chunks) {
            builder.append(chunk).append("\n");
        }
        builder.append("-----END RSA PRIVATE KEY-----\n");

        return builder.toString();
    }

    public static void main(String[] args) {
        //
        System.out.println("\nRSA Key");
        KeyPair keyPair = genRsaKey();
        String publicKey = toPublicKeyFormat(encode(keyPair.getPublic()));
        String privateKey = toPrivateKeyFormat(encode(keyPair.getPrivate()));
        String keyId = TinyUUID.random();
        KeyPair decodedKeyPair = asRsaKey(publicKey, privateKey);
        System.out.println("Key Id      = " + keyId);
        System.out.println("Public Key  = \n" + publicKey);
        System.out.println("Private Key = \n" + privateKey);
        System.out.println("Verify decoded key pair" +
                ": public = " + decodedKeyPair.getPublic().equals(keyPair.getPublic()) +
                ", private = " + decodedKeyPair.getPrivate().equals(keyPair.getPrivate()));
    }
}

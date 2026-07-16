package com.apexads.sdk.integrity;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;

import androidx.annotation.NonNull;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

final class SessionKey {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "apex_ads_trust_session_v1";

    private final PrivateKey privateKey;
    private final String publicKey;
    private final String publicKeyHash;
    private final String keyId;
    private final String securityLevel;
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() * 1000L);
    private final SecureRandom random = new SecureRandom();

    private SessionKey(PrivateKey privateKey,
                       String publicKey,
                       String publicKeyHash,
                       String keyId,
                       String securityLevel) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.publicKeyHash = publicKeyHash;
        this.keyId = keyId;
        this.securityLevel = securityLevel;
    }

    @NonNull
    static SessionKey loadOrCreate() throws Exception {
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        if (!store.containsAlias(ALIAS)) generate(store, true);

        PrivateKey privateKey = (PrivateKey) store.getKey(ALIAS, null);
        byte[] publicDer = store.getCertificate(ALIAS).getPublicKey().getEncoded();
        String publicKey = TrustProtocol.base64Url(publicDer);
        String publicKeyHash = TrustProtocol.sha256(publicDer);
        String keyId = publicKeyHash.substring(0, Math.min(22, publicKeyHash.length()));
        String level = securityLevel(privateKey);
        return new SessionKey(privateKey, publicKey, publicKeyHash, keyId, level);
    }

    private static void generate(KeyStore store, boolean preferStrongBox) throws Exception {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, KEYSTORE);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false);
            if (preferStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setIsStrongBoxBacked(true);
            }
            generator.initialize(builder.build());
            KeyPair ignored = generator.generateKeyPair();
        } catch (StrongBoxUnavailableException e) {
            store.deleteEntry(ALIAS);
            generate(store, false);
        }
    }

    @NonNull String publicKey() { return publicKey; }
    @NonNull String publicKeyHash() { return publicKeyHash; }
    @NonNull String keyId() { return keyId; }
    @NonNull String securityLevel() { return securityLevel; }
    boolean hardwareBacked() {
        return "TEE".equals(securityLevel) || "STRONGBOX".equals(securityLevel);
    }

    @NonNull
    Map<String, String> sign(@NonNull String lease,
                             @NonNull String method,
                             @NonNull String url,
                             @NonNull byte[] body) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        long seq = sequence.incrementAndGet();
        byte[] nonceBytes = new byte[18];
        random.nextBytes(nonceBytes);
        String nonce = TrustProtocol.base64Url(nonceBytes);
        String bodyHash = TrustProtocol.sha256(body);
        String target = TrustProtocol.requestTarget(url);
        String canonical = TrustProtocol.envelope(
                method, target, timestamp, nonce, seq, bodyHash);

        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String signature = TrustProtocol.base64Url(signer.sign());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Apex-Trust-Lease", lease);
        headers.put("X-Apex-Trust-Timestamp", Long.toString(timestamp));
        headers.put("X-Apex-Trust-Nonce", nonce);
        headers.put("X-Apex-Trust-Sequence", Long.toString(seq));
        headers.put("X-Apex-Trust-Body-SHA256", bodyHash);
        headers.put("X-Apex-Trust-Signature", signature);
        return headers;
    }

    @NonNull
    private static String securityLevel(@NonNull PrivateKey privateKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), KEYSTORE);
            KeyInfo info = factory.getKeySpec(privateKey, KeyInfo.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                int level = info.getSecurityLevel();
                if (level == KeyProperties.SECURITY_LEVEL_STRONGBOX) return "STRONGBOX";
                if (level == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT) return "TEE";
                return "SOFTWARE";
            }
            return info.isInsideSecureHardware() ? "TEE" : "SOFTWARE";
        } catch (Exception ignored) {
            return "UNKNOWN";
        }
    }
}

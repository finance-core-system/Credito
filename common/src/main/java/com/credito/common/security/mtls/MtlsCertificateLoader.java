package com.credito.common.security.mtls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

public final class MtlsCertificateLoader {

    private MtlsCertificateLoader() {
    }

    public static KeyStore loadKeyStore(Path path, char[] password, String type) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            KeyStore keyStore = KeyStore.getInstance(type == null || type.isBlank() ? KeyStore.getDefaultType() : type);
            keyStore.load(inputStream, password);

            return keyStore;
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("KeyStore 로딩에 실패했습니다: " + path, exception);
        }
    }

    public static List<X509Certificate> loadX509Certificates(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(inputStream);

            return certificates.stream()
                .filter(X509Certificate.class::isInstance)
                .map(X509Certificate.class::cast)
                .toList();
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("X.509 인증서 로딩에 실패했습니다: " + path, exception);
        }
    }
}

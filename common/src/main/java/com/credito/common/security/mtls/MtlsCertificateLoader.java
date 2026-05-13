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

/**
 * mTLS 구성과 점검에 필요한 인증서 자료를 파일에서 로딩하는 유틸리티 클래스입니다.
 *
 * <p>서비스별 keystore 파일을 {@link KeyStore}로 읽거나, CA bundle 같은 인증서 파일을
 * X.509 certificate 목록으로 변환합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>KeyStore 파일 로딩</li>
 *     <li>X.509 certificate 파일 로딩</li>
 *     <li>인증서 로딩 실패 예외 통일</li>
 * </ul>
 */
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

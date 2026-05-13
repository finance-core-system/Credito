package com.credito.common.security.mtls;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MtlsCertificateLoaderTest {

    @Test
    void wrapsMissingKeyStoreFailure() {
        assertThrows(
            IllegalStateException.class,
            () -> MtlsCertificateLoader.loadKeyStore(Path.of("missing-keystore.p12"), "changeit".toCharArray(), "PKCS12"));
    }
}

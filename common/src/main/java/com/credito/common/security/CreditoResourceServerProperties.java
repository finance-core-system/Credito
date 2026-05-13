package com.credito.common.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credito resource server 보안 설정 값을 바인딩하는 properties 클래스입니다.
 *
 * <p>각 서비스의 설정 파일에서 public path, 허용 audience, 신뢰할 issuer 목록을 읽어
 * {@link CreditoResourceServerSecurity}가 사용할 수 있는 형태로 제공합니다.</p>
 *
 * <p>주요 책임</p>
 * <ul>
 *     <li>인증 없이 허용할 public path 목록 보관</li>
 *     <li>JWT audience 허용 목록 보관</li>
 *     <li>JWT issuer와 JWK set URI 목록 보관</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "credito.security.resource-server")
public class CreditoResourceServerProperties {

    private List<String> publicPaths = new ArrayList<>(List.of("/actuator/health", "/actuator/info"));
    private Set<String> audiences = new LinkedHashSet<>();
    private List<Issuer> issuers = new ArrayList<>();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public Set<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(Set<String> audiences) {
        this.audiences = audiences;
    }

    public List<Issuer> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<Issuer> issuers) {
        this.issuers = issuers;
    }

    /**
     * 단일 JWT issuer 설정을 표현하는 properties 클래스입니다.
     *
     * <p>issuer URI와 해당 issuer의 공개키를 조회할 JWK set URI를 함께 보관합니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>JWT issuer URI 보관</li>
     *     <li>JWK set URI 보관</li>
     * </ul>
     */
    public static class Issuer {

        private String issuerUri;
        private String jwkSetUri;

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }
    }
}

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
 *     <li>내부 API service-token 정책 보관</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "credito.security.resource-server")
public class CreditoResourceServerProperties {

    private List<String> publicPaths = new ArrayList<>(List.of("/actuator/health", "/actuator/info"));
    private Set<String> audiences = new LinkedHashSet<>();
    private List<Issuer> issuers = new ArrayList<>();
    private ServiceToken serviceToken = new ServiceToken();

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

    public ServiceToken getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(ServiceToken serviceToken) {
        this.serviceToken = serviceToken;
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

    /**
     * 내부 서비스 간 service-token 검증 정책을 표현하는 properties 클래스입니다.
     *
     * <p>검증 대상 issuer, audience와 내부 API path별 허용 client 및 필수 scope를 보관합니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>service-token 검증 활성화 여부 보관</li>
     *     <li>허용 issuer와 audience 목록 보관</li>
     *     <li>path별 허용 client와 필수 scope 규칙 보관</li>
     * </ul>
     */
    public static class ServiceToken {

        private boolean enabled = true;
        private boolean requireClientCertificate = true;
        private Set<String> allowedIssuers = new LinkedHashSet<>();
        private Set<String> allowedAudiences = new LinkedHashSet<>();
        private List<Rule> rules = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequireClientCertificate() {
            return requireClientCertificate;
        }

        public void setRequireClientCertificate(boolean requireClientCertificate) {
            this.requireClientCertificate = requireClientCertificate;
        }

        public Set<String> getAllowedIssuers() {
            return allowedIssuers;
        }

        public void setAllowedIssuers(Set<String> allowedIssuers) {
            this.allowedIssuers = allowedIssuers;
        }

        public Set<String> getAllowedAudiences() {
            return allowedAudiences;
        }

        public void setAllowedAudiences(Set<String> allowedAudiences) {
            this.allowedAudiences = allowedAudiences;
        }

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
    }

    /**
     * 단일 내부 API service-token 허용 규칙을 표현하는 properties 클래스입니다.
     *
     * <p>path pattern에 대해 호출 가능한 service client id와 필요한 scope를 보관합니다.</p>
     *
     * <p>주요 책임</p>
     * <ul>
     *     <li>보호할 path pattern 보관</li>
     *     <li>허용 client id 목록 보관</li>
     *     <li>필수 scope 목록 보관</li>
     * </ul>
     */
    public static class Rule {

        private String pathPattern;
        private Set<String> allowedClientIds = new LinkedHashSet<>();
        private Set<String> requiredScopes = new LinkedHashSet<>();

        public String getPathPattern() {
            return pathPattern;
        }

        public void setPathPattern(String pathPattern) {
            this.pathPattern = pathPattern;
        }

        public Set<String> getAllowedClientIds() {
            return allowedClientIds;
        }

        public void setAllowedClientIds(Set<String> allowedClientIds) {
            this.allowedClientIds = allowedClientIds;
        }

        public Set<String> getRequiredScopes() {
            return requiredScopes;
        }

        public void setRequiredScopes(Set<String> requiredScopes) {
            this.requiredScopes = requiredScopes;
        }
    }
}

package com.ocbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Opcbs.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final OAuth2 oauth2 = new OAuth2();

    private final Websocket websocket = new Websocket();

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Websocket getWebsocket() {
        return websocket;
    }

    public static class Websocket {

        private java.util.List<String> allowedOrigins = new java.util.ArrayList<>(java.util.List.of(
            "http://localhost:9000",
            "http://localhost:8080",
            "http://localhost",
            "https://localhost",
            "http://127.0.0.1:9000",
            "http://127.0.0.1:8080"
        ));
        private int messageSizeLimit = 64 * 1024; // 64 KB
        private int sendBufferSizeLimit = 512 * 1024; // 512 KB
        private int sendTimeLimit = 20 * 1000; // 20s

        public java.util.List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(java.util.List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public int getMessageSizeLimit() {
            return messageSizeLimit;
        }

        public void setMessageSizeLimit(int messageSizeLimit) {
            this.messageSizeLimit = messageSizeLimit;
        }

        public int getSendBufferSizeLimit() {
            return sendBufferSizeLimit;
        }

        public void setSendBufferSizeLimit(int sendBufferSizeLimit) {
            this.sendBufferSizeLimit = sendBufferSizeLimit;
        }

        public int getSendTimeLimit() {
            return sendTimeLimit;
        }

        public void setSendTimeLimit(int sendTimeLimit) {
            this.sendTimeLimit = sendTimeLimit;
        }
    }

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    public static class OAuth2 {

        private final Google google = new Google();

        public Google getGoogle() {
            return google;
        }

        public static class Google {

            private String clientId = "";
            private String clientSecret = "";

            public String getClientId() {
                return clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getClientSecret() {
                return clientSecret;
            }

            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }
        }
    }
}

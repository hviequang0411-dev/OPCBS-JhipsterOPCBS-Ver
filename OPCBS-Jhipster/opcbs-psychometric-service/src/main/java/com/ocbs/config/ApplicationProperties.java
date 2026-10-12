package com.ocbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Opcbs.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final OAuth2 oauth2 = new OAuth2();

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public OAuth2 getOauth2() {
        return oauth2;
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

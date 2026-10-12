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

    private final Cloudinary cloudinary = new Cloudinary();

    private final FileStorage fileStorage = new FileStorage();

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public Cloudinary getCloudinary() {
        return cloudinary;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
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

    public static class FileStorage {

        private String uploadDir = "./uploads/certificates";

        public String getUploadDir() {
            return uploadDir;
        }

        public void setUploadDir(String uploadDir) {
            this.uploadDir = uploadDir;
        }
    }

    public static class Cloudinary {

        private String cloudName = "jqhp5g23";
        private String apiKey = "136888838371782";
        private String apiSecret = "utJ16iRSPhJE3xaHVsDDmvExZ9E";

        public String getCloudName() {
            return cloudName;
        }

        public void setCloudName(String cloudName) {
            this.cloudName = cloudName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
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

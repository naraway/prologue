package io.naraway.prologue.autoconfigure;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@Data
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocProperties {
    //
    @Value("${spring.application.name:drama}")
    private String title;

    private String version;
    private InfoProperties info;
    private SwaggerUiConfigProperties swaggerUi;
    private SpringDocConfigProperties.ApiDocs apiDocs;

    public SpringDocProperties() {
        //
        this.version = "Unknown";
        this.info = new InfoProperties();
        this.swaggerUi = new SwaggerUiConfigProperties();
        this.apiDocs = new SpringDocConfigProperties.ApiDocs();
    }

    @Data
    public static class InfoProperties {
        //
        private String name;
        private String email;
        private String url;

        public InfoProperties() {
            //
            this.name = "Nara Way";
            this.email = "naraway@nextree.io";
            this.url = "http://naraway.io";
        }
    }
}

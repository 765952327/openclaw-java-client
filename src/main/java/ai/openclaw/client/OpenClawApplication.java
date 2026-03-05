package ai.openclaw.client;

import ai.openclaw.client.config.OpenClawProperties;
import ai.openclaw.client.scheduler.OpenClawScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenClawProperties.class)
public class OpenClawApplication {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OpenClawApplication.class, args);
    }

    @Bean
    public OpenClawClient openClawClient(OpenClawProperties properties) {
        return new OpenClawClient(properties);
    }

    @Bean
    public OpenClawScheduler openClawScheduler(OpenClawClient openClawClient, OpenClawProperties properties) {
        return new OpenClawScheduler(openClawClient, properties);
    }
}

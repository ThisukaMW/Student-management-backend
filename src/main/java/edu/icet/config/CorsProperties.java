package edu.icet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/** Origins allowed to call the API from a browser, set with {@code app.cors.allowed-origins}. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5500", "http://localhost:5500"})
        List<String> allowedOrigins
) {
}

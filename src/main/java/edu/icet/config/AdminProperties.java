package edu.icet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** First-admin credentials from ADMIN_EMAIL / ADMIN_PASSWORD (env or .env). Both optional. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {

    public boolean isConfigured() {
        return StringUtils.hasText(email) && StringUtils.hasText(password);
    }

    // Keep the password out of logs
    @Override
    public String toString() {
        return "AdminProperties[email=" + email + "]";
    }
}

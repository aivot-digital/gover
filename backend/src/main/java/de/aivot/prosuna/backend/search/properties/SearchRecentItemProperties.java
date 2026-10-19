package de.aivot.prosuna.backend.search.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "prosuna.search.recent")
@Validated
public class SearchRecentItemProperties {
    @NotNull
    @Min(1)
    private Integer maxItemsPerUser;

    @NotNull
    @Min(1)
    private Integer retentionDays;

    public Integer getMaxItemsPerUser() {
        return maxItemsPerUser;
    }

    public void setMaxItemsPerUser(Integer maxItemsPerUser) {
        this.maxItemsPerUser = maxItemsPerUser;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }
}

package de.aivot.gover.backend.models.config;

import de.aivot.gover.backend.core.enums.ModuleFlags;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "gover")
public class GoverConfig {
    private String fromMail;
    private List<String> reportMail;
    private String sentryServer;
    private String sentryWebApp;
    private String environment;
    private List<String> departmentLevelLabels;
    @Deprecated
    private List<String> fileExtensions;
    @Deprecated
    private List<String> contentTypes;
    private String goverHostname;
    private Integer maxSubmissionCopyRetryCount;
    private List<String> bootstrapAdminMail;
    private String registryHostname;
    private String timezone;

    private Map<ProcessNodeType, Integer> processNodeLimits;
    private List<ModuleFlags> moduleFlags;


    public String getDefaultLogoUrl() {
        return createUrl("/assets/default-logo.png");
    }

    public String getDefaultFaviconUrl() {
        return createUrl("/assets/default-favicon.ico");
    }

    public String createUrl(String path) {
        var uri = URI.create(goverHostname);
        return uri.resolve(path).toString();
    }

    public String createUrl(String base, Object... parts) {
        var uri = URI.create(goverHostname);

        var resolvedParts = Arrays
                .stream(parts)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        if (base.endsWith("/")) {
            return uri.resolve(base + resolvedParts).toString();
        } else {
            return uri.resolve(base + "/" + resolvedParts).toString();
        }
    }

    public String createUrlWithTrailingSlash(String base, Object... parts) {
        var url = createUrl(base, parts);
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    // region Getters & Setters

    public String getFromMail() {
        return fromMail;
    }

    public void setFromMail(String fromMail) {
        this.fromMail = fromMail;
    }

    public List<String> getReportMail() {
        return reportMail;
    }

    public void setReportMail(List<String> reportMail) {
        this.reportMail = reportMail;
    }

    public String getSentryServer() {
        return sentryServer;
    }

    public void setSentryServer(String sentryServer) {
        this.sentryServer = sentryServer;
    }

    public String getSentryWebApp() {
        return sentryWebApp;
    }

    public void setSentryWebApp(String sentryWebApp) {
        this.sentryWebApp = sentryWebApp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getDepartmentLevelLabels() {
        return departmentLevelLabels;
    }

    public void setDepartmentLevelLabels(List<String> departmentLevelLabels) {
        this.departmentLevelLabels = departmentLevelLabels;
    }

    public List<String> getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public String getGoverHostname() {
        return goverHostname;
    }

    public void setGoverHostname(String goverHostname) {
        this.goverHostname = goverHostname;
    }

    public Integer getMaxSubmissionCopyRetryCount() {
        return maxSubmissionCopyRetryCount;
    }

    public void setMaxSubmissionCopyRetryCount(Integer maxSubmissionCopyRetryCount) {
        this.maxSubmissionCopyRetryCount = maxSubmissionCopyRetryCount;
    }

    public List<String> getBootstrapAdminMail() {
        return bootstrapAdminMail;
    }

    public void setBootstrapAdminMail(List<String> bootstrapAdminMail) {
        this.bootstrapAdminMail = bootstrapAdminMail;
    }

    public String getRegistryHostname() {
        return registryHostname;
    }

    public GoverConfig setRegistryHostname(String registryHostname) {
        this.registryHostname = registryHostname;
        return this;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    public boolean hasModuleFlag(ModuleFlags flag) {
        return moduleFlags != null && moduleFlags.contains(flag);
    }

    public boolean isFormModuleEnabled() {
        return hasModuleFlag(ModuleFlags.FORM);
    }

    public boolean isProcessModuleEnabled() {
        return hasModuleFlag(ModuleFlags.PROCESS);
    }

    /**
     * Missing limits are treated as unlimited so adding a new process node type or running with older
     * configuration does not silently turn into a zero-capacity system.
     */
    public int getProcessNodeLimit(ProcessNodeType type) {
        if (processNodeLimits == null) {
            return -1;
        }

        var limit = processNodeLimits.get(type);
        return limit != null ? limit : -1;
    }

    public boolean isProcessNodeTypeUnlimited(ProcessNodeType type) {
        return isProcessModuleEnabled() || getProcessNodeLimit(type) < 0;
    }

    public List<ModuleFlags> getModuleFlags() {
        if (moduleFlags == null) {
            return new LinkedList<>();
        }
        return moduleFlags;
    }

    public GoverConfig setModuleFlags(List<ModuleFlags> moduleFlags) {
        this.moduleFlags = moduleFlags;
        return this;
    }

    public Map<ProcessNodeType, Integer> getProcessNodeLimits() {
        if (processNodeLimits == null) {
            return new HashMap<>();
        }
        return processNodeLimits;
    }

    public GoverConfig setProcessNodeLimits(Map<ProcessNodeType, Integer> processNodeLimits) {
        this.processNodeLimits = processNodeLimits;
        return this;
    }

    // endregion
}

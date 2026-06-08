package de.aivot.GoverBackend.plugins.ai.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties shared by the AI plugin nodes.
 */
@Configuration
@ConfigurationProperties(prefix = "gover.ai")
@Validated
public class AiPluginProperties {
    @Min(1)
    private int defaultMaxTokens = 1000;

    @Valid
    private CompletionProperties completion = new CompletionProperties();

    @Valid
    private ProcessDataTransformationProperties processDataTransformation = new ProcessDataTransformationProperties();

    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    public void setDefaultMaxTokens(int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    public CompletionProperties getCompletion() {
        return completion;
    }

    public void setCompletion(CompletionProperties completion) {
        this.completion = completion != null ? completion : new CompletionProperties();
    }

    public ProcessDataTransformationProperties getProcessDataTransformation() {
        return processDataTransformation;
    }

    public void setProcessDataTransformation(ProcessDataTransformationProperties processDataTransformation) {
        this.processDataTransformation = processDataTransformation != null
                ? processDataTransformation
                : new ProcessDataTransformationProperties();
    }

    public int getCompletionMaxTokens() {
        return completion.getMaxTokens() != null
                ? completion.getMaxTokens()
                : defaultMaxTokens;
    }

    public int getProcessDataTransformationMaxTokens() {
        return processDataTransformation.getMaxTokens() != null
                ? processDataTransformation.getMaxTokens()
                : defaultMaxTokens;
    }

    public static class CompletionProperties {
        @Min(1)
        private Integer maxTokens;

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class ProcessDataTransformationProperties {
        @Min(1)
        private Integer maxTokens;

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}

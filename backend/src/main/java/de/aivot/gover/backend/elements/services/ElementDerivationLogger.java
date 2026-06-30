package de.aivot.gover.backend.elements.services;

import de.aivot.gover.backend.elements.enums.ElementDerivationLogLevel;
import de.aivot.gover.backend.elements.exceptions.DerivationException;
import de.aivot.gover.backend.elements.models.ElementDerivationLogItem;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.javascript.models.JavascriptResult;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ElementDerivationLogger {
    private final List<ElementDerivationLogItem> items = new ArrayList<>();

    public void log(@Nonnull BaseElement element, @Nullable JavascriptResult result) {
        if (result == null) {
            return;
        }

        var errOutput = result.getErrOutput();
        if (StringUtils.isNotNullOrEmpty(errOutput)) {
            this.error(element, errOutput);
        }

        var stdOutput = result.getStdOutput();
        if (StringUtils.isNotNullOrEmpty(stdOutput)) {
            this.debug(element, stdOutput);
        }
    }

    public void debug(@Nonnull BaseElement baseElement, @Nonnull String message) {
        debug(baseElement, message, null);
    }

    public void debug(@Nonnull BaseElement baseElement, @Nonnull String message, @Nullable Map<String, Object> details) {
        var item = new ElementDerivationLogItem(
                baseElement.getId(),
                ElementDerivationLogLevel.Debug,
                message,
                details
        );
        items.add(item);
    }

    public void error(@Nonnull DerivationException e) {
        error(e.baseElement, e);
    }

    public void error(@Nonnull BaseElement baseElement, Throwable e) {
        error(baseElement, e.getMessage(), Map.of("stackTrace", e.getStackTrace()));
    }

    public void error(@Nonnull BaseElement baseElement, @Nonnull String message) {
        error(baseElement, message, null);
    }

    public void error(@Nonnull BaseElement baseElement, @Nonnull String message, @Nullable Map<String, Object> details) {
        var item = new ElementDerivationLogItem(
                baseElement.getId(),
                ElementDerivationLogLevel.Error,
                message,
                details
        );
        items.add(item);
    }

    public List<ElementDerivationLogItem> getLogItems() {
        return items;
    }
}

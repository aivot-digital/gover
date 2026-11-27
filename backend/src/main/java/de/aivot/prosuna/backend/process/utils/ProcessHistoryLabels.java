package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** Shared display names; Markdown callers must escape the resolved plain text. */
public final class ProcessHistoryLabels {
    private ProcessHistoryLabels() {
    }

    @Nonnull
    public static String nameOrId(@Nonnull Object id, @Nullable Object name) {
        var title = name == null ? null : StringUtils.toNullableTrimmedString(name.toString());
        return title == null ? id.toString() : title;
    }

    @Nonnull
    public static String quotedUser(@Nonnull UserEntity user) {
        return StringUtils.quote(nameOrId(user.getId(), user.getFullName()));
    }
}

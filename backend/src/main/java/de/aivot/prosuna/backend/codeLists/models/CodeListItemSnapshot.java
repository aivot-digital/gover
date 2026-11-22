package de.aivot.prosuna.backend.codeLists.models;

import de.aivot.prosuna.backend.codeLists.entities.CodeListItemEntity;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;

/**
 * A row of a code list, read in one statement together with the columns its list had at that moment. A
 * synchronization can reorder the columns between reading a list and reading its rows, so the mappings of the list a
 * caller holds only fit the cells after they are moved into that list's column order.
 */
public record CodeListItemSnapshot(
        @Nonnull Long id,
        @Nonnull Integer codeListId,
        @Nonnull List<String> columns,
        @Nonnull Instant created,
        @Nonnull Instant updated,
        @Nonnull List<String> listColumns
) {
    /** Returns the row with its cells in the column order {@code columnOrder}, as a detached item. */
    @Nonnull
    public CodeListItemEntity toItem(@Nonnull List<String> columnOrder) {
        var item = new CodeListItemEntity()
                .setId(id)
                .setCodeListId(codeListId)
                .setColumns(columns)
                .setCreated(created)
                .setUpdated(updated);
        return columnOrder.equals(listColumns)
                ? item
                : item.setColumns(item.cellsInColumnOrder(listColumns, columnOrder));
    }
}

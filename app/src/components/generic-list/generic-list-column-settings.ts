import {type GridColDef, type GridInitialState} from '@mui/x-data-grid';
import {StorageKey} from '../../data/storage-key';
import {StorageScope, StorageService} from '../../services/storage-service';

type ColumnSettings = NonNullable<GridInitialState['columns']>;

function isRecord(value: unknown): value is Record<string, unknown> {
    return value != null && typeof value === 'object' && !Array.isArray(value);
}

export function loadListColumnSettings(
    key: StorageKey | undefined,
    columns: readonly GridColDef[],
    defaults: Record<string, boolean> = {},
): ColumnSettings {
    const result: ColumnSettings = {
        columnVisibilityModel: {...defaults},
        dimensions: {},
    };
    try {
        const stored = key == null ? null : StorageService.loadObject<unknown>(key);
        if (!isRecord(stored)) return result;
        for (const column of columns) {
            const visible = isRecord(stored.columnVisibilityModel)
                ? stored.columnVisibilityModel[column.field]
                : undefined;
            if (column.hideable === false) result.columnVisibilityModel![column.field] = true;
            else if (typeof visible === 'boolean') result.columnVisibilityModel![column.field] = visible;
            const dimension = isRecord(stored.dimensions) ? stored.dimensions[column.field] : undefined;
            if (
                column.resizable !== false &&
                isRecord(dimension) &&
                typeof dimension.width === 'number' &&
                Number.isFinite(dimension.width) &&
                dimension.width > 0
            ) {
                // Preserve current min/max constraints; a resized flex column becomes a fixed-width column.
                result.dimensions![column.field] = {
                    width: dimension.width,
                    flex: 0,
                };
            }
        }
    } catch {
        // Browser storage can be unavailable; column configuration must still work for this visit.
    }
    return result;
}

export function saveListColumnSettings(
    key: StorageKey | undefined,
    columns: readonly GridColDef[],
    settings: ColumnSettings | undefined,
): void {
    if (key == null || settings == null) return;
    try {
        const stored = StorageService.loadObject<unknown>(key);
        StorageService.storeObject(
            key,
            {
                fullWidth: isRecord(stored) && stored.fullWidth === true,
                dimensions: settings.dimensions,
                // Explicit values distinguish "show all" from defaults for new columns in future versions.
                columnVisibilityModel: Object.fromEntries(
                    columns.map((column) => [
                        column.field,
                        column.hideable === false || settings.columnVisibilityModel?.[column.field] !== false,
                    ]),
                ),
            },
            StorageScope.Local,
        );
    } catch {
        // Keep the native grid state even when persisting it is not possible.
    }
}

export function loadListFullWidth(key: StorageKey | undefined): boolean {
    try {
        const stored = key == null ? null : StorageService.loadObject<unknown>(key);
        return isRecord(stored) && stored.fullWidth === true;
    } catch {
        return false;
    }
}

export function saveListFullWidth(key: StorageKey | undefined, fullWidth: boolean): void {
    if (key == null) return;
    try {
        const stored = StorageService.loadObject<unknown>(key);
        StorageService.storeObject(
            key,
            {
                ...(isRecord(stored) ? stored : {}),
                fullWidth,
            },
            StorageScope.Local,
        );
    } catch {
        // The display toggle also works when browser storage is unavailable.
    }
}

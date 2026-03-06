import {createElement, ReactElement} from 'react';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import {BaseApiService} from '../../services/base-api-service';
import {DataObjectItem} from '../../modules/data-objects/models/data-object-item';
import {Page} from '../../models/dtos/page';
import {ElementDataObject} from '../../models/element-data';

export interface DataObjectSelectOption {
    key: string;
    value: string;
    label: string;
    subLabel?: string;
    icon?: ReactElement;
}

const cachedItems = new Map<string, DataObjectItem[]>();
const cachedItemsPromises = new Map<string, Promise<DataObjectItem[]>>();
const PAGE_SIZE = 250;

export function normalizeDataObjectId(value: unknown): string | undefined {
    if (typeof value !== 'string') {
        return undefined;
    }

    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
}

export function formatDataObjectSelectValue(value: string): string {
    return `Datenobjekt #${value}`;
}

export function formatDataObjectSelectSubLabel(dataModelKey: string, dataObjectId: string): string {
    return `${dataModelKey} · ${dataObjectId}`;
}

export function normalizeDataLabelAttributeKey(value: unknown): string | undefined {
    if (typeof value !== 'string') {
        return undefined;
    }

    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
}

export async function loadDataObjectSelectOptions(
    dataModelKey: string,
    dataLabelAttributeKey?: string,
    forceReload: boolean = false,
): Promise<DataObjectSelectOption[]> {
    const normalizedLabelAttributeKey = normalizeDataLabelAttributeKey(dataLabelAttributeKey);
    const items = await loadDataObjectItems(dataModelKey, forceReload);
    return items
        .map((item) => {
            const id = item.id;
            const primaryLabel = extractPrimaryLabel(item, normalizedLabelAttributeKey);

            return {
                key: id,
                value: id,
                label: primaryLabel,
                subLabel: formatDataObjectSelectSubLabel(dataModelKey, id),
                icon: createElement(DataObject),
            } satisfies DataObjectSelectOption;
        })
        .sort((a, b) => {
            const labelDiff = a.label.localeCompare(b.label, 'de');
            if (labelDiff !== 0) {
                return labelDiff;
            }

            return a.value.localeCompare(b.value, 'de');
        });
}

function extractPrimaryLabel(
    item: DataObjectItem,
    dataLabelAttributeKey?: string,
): string {
    if (dataLabelAttributeKey == null) {
        return item.id;
    }

    const entry = item.data?.[dataLabelAttributeKey] as ElementDataObject | undefined;
    const inputValue = entry?.inputValue;

    const primitiveLabel = formatPrimitiveLabelValue(inputValue);
    if (primitiveLabel != null) {
        return primitiveLabel;
    }

    if (Array.isArray(inputValue)) {
        const values = inputValue
            .map((value) => formatPrimitiveLabelValue(value))
            .filter((value): value is string => value != null);

        if (values.length > 0) {
            return values.join(', ');
        }
    }

    return item.id;
}

function formatPrimitiveLabelValue(value: unknown): string | undefined {
    if (typeof value === 'string') {
        const normalized = value.trim();
        return normalized.length > 0 ? normalized : undefined;
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }

    return undefined;
}

async function loadDataObjectItems(
    dataModelKey: string,
    forceReload: boolean = false,
): Promise<DataObjectItem[]> {
    if (!forceReload && cachedItems.has(dataModelKey)) {
        return cachedItems.get(dataModelKey) ?? [];
    }

    if (!forceReload && cachedItemsPromises.has(dataModelKey)) {
        return cachedItemsPromises.get(dataModelKey) ?? [];
    }

    const api = new BaseApiService();
    const path = `/api/data-objects/${encodeURIComponent(dataModelKey)}/items/`;
    const promise = listAllPages<DataObjectItem>((page) => {
        return api.get<Page<DataObjectItem>>(path, {
            query: {
                page,
                size: PAGE_SIZE,
            },
        });
    })
        .then((items) => {
            cachedItems.set(dataModelKey, items);
            return items;
        })
        .finally(() => {
            cachedItemsPromises.delete(dataModelKey);
        });

    cachedItemsPromises.set(dataModelKey, promise);
    return promise;
}

async function listAllPages<T>(
    fetchPage: (page: number) => Promise<{
        content: T[];
        last: boolean;
    }>,
): Promise<T[]> {
    const items: T[] = [];
    let currentPage = 0;
    let hasMore = true;

    while (hasMore) {
        const response = await fetchPage(currentPage);
        items.push(...response.content);
        hasMore = !response.last;
        currentPage += 1;
    }

    return items;
}

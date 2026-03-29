import {createElement, ReactElement} from 'react';
import FolderData from '@aivot/mui-material-symbols-400-outlined/dist/folder-data/FolderData';
import {BaseApiService} from '../../services/base-api-service';
import {DataObjectSchema} from '../../modules/data-objects/models/data-object-schema';
import {Page} from '../../models/dtos/page';

export interface DataModelSelectOption {
    key: string;
    value: string;
    label: string;
    subLabel?: string;
    icon?: ReactElement;
}

let cachedOptions: DataModelSelectOption[] | undefined;
let cachedOptionsPromise: Promise<DataModelSelectOption[]> | undefined;
const PAGE_SIZE = 250;

export function normalizeDataModelKey(value: unknown): string | undefined {
    if (typeof value !== 'string') {
        return undefined;
    }

    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
}

export function formatDataModelSelectValue(value: string): string {
    return `Datenmodell #${value}`;
}

export async function loadDataModelSelectOptions(forceReload: boolean = false): Promise<DataModelSelectOption[]> {
    if (!forceReload && cachedOptions != null) {
        return cachedOptions;
    }

    if (!forceReload && cachedOptionsPromise != null) {
        return cachedOptionsPromise;
    }

    const api = new BaseApiService();
    const promise = listAllPages<DataObjectSchema>((page) => {
        return api.get<Page<DataObjectSchema>>('/api/data-objects/', {
            query: {
                page,
                size: PAGE_SIZE,
            },
        });
    })
        .then((models) => {
            const options = models
                .map((model) => {
                    const key = model.key;
                    return {
                        key,
                        value: key,
                        label: model.name?.trim().length > 0 ? model.name : formatDataModelSelectValue(key),
                        subLabel: key,
                        icon: createElement(FolderData),
                    } satisfies DataModelSelectOption;
                })
                .sort((a, b) => {
                    const nameDiff = a.label.localeCompare(b.label, 'de');
                    if (nameDiff !== 0) {
                        return nameDiff;
                    }

                    return a.key.localeCompare(b.key, 'de');
                });

            cachedOptions = options;
            return options;
        })
        .finally(() => {
            cachedOptionsPromise = undefined;
        });

    cachedOptionsPromise = promise;
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

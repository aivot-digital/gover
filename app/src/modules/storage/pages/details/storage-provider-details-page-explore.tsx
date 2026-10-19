import React, {type ReactNode} from 'react';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {type StorageProviderEntity} from '../../entities/storage-provider-entity';
import {type StorageProviderAdditionalData} from './storage-provider-details-page-additional-data';
import {StorageExplorer} from '../../components/storage-explorer';

export function StorageProviderDetailsPageExplore(): ReactNode {
    const {
        item,
    } = useGenericDetailsPageContext<StorageProviderEntity, StorageProviderAdditionalData>();

    if (item == null) {
        return null;
    }

    return (
        <StorageExplorer
            providerId={item.id}
            showTopNavigationBar
        />
    );
}

import React from 'react';
import {cloneElement} from '../../../utils/clone-element';
import {type BaseTabProps} from './base-tab-props';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import {StoreModuleList} from '../../../components/store-module-list/store-module-list';

export function StoreTab(props: BaseTabProps & {
    showModuleId: (id: string) => void;
    highlightedModuleId?: string;
}): JSX.Element {
    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    return (
        <StoreModuleList
            onSelect={(module, element) => {
                const elementToAdd = cloneElement(element, true);
                elementToAdd.storeLink = {
                    storeId: module.id,
                    storeVersion: module.current_version,
                };
                elementToAdd.name = module.title.substring(0, 30);
                props.onAddElement(elementToAdd);
            }}
            storeKey={storeKey}
            itemAction={{
                icon: <InfoOutlinedIcon/>,
                tooltip: 'Mehr Informationen',
                onClick: (module) => {
                    props.showModuleId(module.id);
                },
            }}
            selectedModuleId={props.highlightedModuleId}
        />
    );
}

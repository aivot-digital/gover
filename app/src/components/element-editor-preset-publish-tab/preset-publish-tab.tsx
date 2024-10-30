import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Box, Tab, Tabs, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type PresetVersion} from '../../models/entities/preset-version';
import {type Preset} from '../../models/entities/preset';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {PresetInternalPublishTab} from './preset-internal-publish-tab';
import {PresetStorePublishTab} from './preset-store-publish-tab';
import {useApi} from "../../hooks/use-api";
import {usePresetsApi} from "../../hooks/use-presets-api";

export function PresetPublishTab(props: ElementEditorContentProps<GroupLayout, PresetVersion>): JSX.Element {
    const api = useApi();
    const [preset, setPreset] = useState<Preset>();

    const [currentTab, setCurrentTab] = useState('internal');

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    useEffect(() => {
        if (props.entity != null && (preset == null || preset.key !== props.entity.preset)) {
            usePresetsApi(api)
                .retrieve(props.entity.preset)
                .then(setPreset);
        }
    }, [props.entity]);

    if (preset == null) {
        return (
            <Box>
                <Typography>
                    Laden...
                </Typography>
            </Box>
        );
    }

    return (
        <Box>
            {
                storeKey != null &&
                isStringNotNullOrEmpty(storeKey) &&
                <Tabs
                    value={currentTab}
                    onChange={(_, newValue) => {
                        setCurrentTab(newValue);
                    }}
                >
                    <Tab
                        label="Lokal"
                        value="internal"
                    />

                    <Tab
                        label="Store"
                        value="store"
                    />
                </Tabs>
            }

            {
                currentTab === 'internal' &&
                <PresetInternalPublishTab
                    preset={preset}
                    onPresetChange={setPreset}
                    storeKey={storeKey}
                    {...props} />
            }

            {
                currentTab === 'store' &&
                <PresetStorePublishTab
                    preset={preset}
                    onPresetChange={setPreset}
                    storeKey={storeKey}
                    {...props} />
            }
        </Box>
    );
}

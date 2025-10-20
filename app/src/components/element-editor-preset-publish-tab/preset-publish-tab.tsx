import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Box, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type PresetVersion} from '../../models/entities/preset-version';
import {type Preset} from '../../models/entities/preset';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {PresetInternalPublishTab} from './preset-internal-publish-tab';
import {useApi} from '../../hooks/use-api';
import {PresetsApiService} from '../../modules/presets/presets-api-service';

export function PresetPublishTab(props: ElementEditorContentProps<GroupLayout, PresetVersion>) {
    const api = useApi();
    const [preset, setPreset] = useState<Preset>();

    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));
    const presetsApiService = new PresetsApiService(api);

    useEffect(() => {
        if (props.entity != null && (preset == null || preset.key !== props.entity.presetKey)) {
            presetsApiService.retrieve(props.entity.presetKey)
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
            <PresetInternalPublishTab
                preset={preset}
                onPresetChange={setPreset}
                storeKey={storeKey}
                {...props} />
        </Box>
    );
}

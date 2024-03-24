import {Box, Button, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type PresetVersion} from '../../models/entities/preset-version';
import {type StoreDetailModule} from '../../models/entities/store-detail-module';
import {GoverStoreService} from '../../services/gover-store.service';
import {AlertComponent} from '../alert/alert-component';
import {PublishPresetDialog} from '../../dialogs/preset-dialogs/publish-preset-dialog/publish-preset-dialog';
import {StoreModuleInfoTable} from '../store-module-info-table/store-module-info-table';
import {PublishPresetVersionDialog} from '../../dialogs/preset-dialogs/publish-preset-version-dialog/publish-preset-version-dialog';
import {type TabProps} from './tab-props';
import {type Preset} from '../../models/entities/preset';
import {useApi} from "../../hooks/use-api";
import {usePresetsApi} from "../../hooks/use-presets-api";

export function PresetStorePublishTab(props: TabProps<GroupLayout, PresetVersion>): JSX.Element {
    const api = useApi();
    const [storeModule, setStoreModule] = useState<StoreDetailModule>();
    const [showPublishDialog, setShowPublishDialog] = useState(false);

    useEffect(() => {
        if (props.preset.storeId != null && props.storeKey != null && (storeModule == null || storeModule.id !== props.preset.storeId)) {
            GoverStoreService
                .fetchModule(props.preset.storeId, props.storeKey)
                .then(setStoreModule);
        }
    }, [props.preset, props.storeKey]);

    const isNewModule = storeModule == null;
    const isExistingModule = !isNewModule;
    const isNewModuleVersion = storeModule != null && !storeModule.versions.includes(props.entity.version);

    const handlePublishNewPreset = async (storeModule: StoreDetailModule): Promise<void> => {
        const updatedPreset: Preset = {
            ...props.preset,
            storeId: storeModule.id,
            currentStoreVersion: props.entity.version,
        };
        const updatedVersion: PresetVersion = {
            ...props.entity,
            publishedStoreAt: new Date().toISOString(),
        };

        await usePresetsApi(api)
            .save(updatedPreset);
        await usePresetsApi(api)
            .updateVersion(props.preset.key, updatedVersion);

        props.onPresetChange(updatedPreset);
        props.onChangeEntity(updatedVersion);

        setStoreModule(storeModule);
        setShowPublishDialog(false);

        window.location.reload();
    };

    const handlePublishNewVersion = async (storeModule: StoreDetailModule): Promise<void> => {
        const updatedPreset: Preset = {
            ...props.preset,
            currentStoreVersion: props.entity.version,
        };
        const updatedVersion: PresetVersion = {
            ...props.entity,
            publishedStoreAt: new Date().toISOString(),
        };

        await usePresetsApi(api)
            .save(updatedPreset);
        await usePresetsApi(api)
            .updateVersion(props.preset.key, updatedVersion);

        props.onPresetChange(updatedPreset);
        props.onChangeEntity(updatedVersion);

        setStoreModule(storeModule);
        setShowPublishDialog(false);

        window.location.reload();
    };

    if (props.entity.publishedAt == null) {
        return (
            <Box sx={{mt: 3}}>
                <AlertComponent
                    color="info"
                    title={`Lokale Version ${props.entity.version} unveröffentlicht`}
                >
                    Bitte veröffentlichen Sie die Version {props.entity.version} erst lokal. Danach kann die Version im Store veröffentlicht werden.
                </AlertComponent>
            </Box>
        );
    }

    return (
        <>
            <Box sx={{mt: 3}}>
                {
                    isNewModule &&
                    <AlertComponent
                        color="info"
                        title="Nicht im Store veröffentlicht"
                    >
                        Diese Vorlage wurde noch nicht im Store veröffentlicht.
                    </AlertComponent>
                }

                {
                    isExistingModule &&
                    <StoreModuleInfoTable
                        module={storeModule}
                    />
                }

                {
                    (isNewModule || (isExistingModule && isNewModuleVersion)) &&
                    <>
                        <Typography
                            variant="h6"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Vorlage Veröffentlichen
                        </Typography>

                        <Box
                            sx={{
                                mt: 4,
                            }}
                        >
                            {
                                isNewModule &&
                                <Button
                                    variant="contained"
                                    onClick={() => {
                                        setShowPublishDialog(true);
                                    }}
                                >
                                    Veröffentlichen
                                </Button>
                            }

                            {
                                isNewModuleVersion &&
                                <Button
                                    variant="contained"
                                    onClick={() => {
                                        setShowPublishDialog(true);
                                    }}
                                >
                                    Neue Version Veröffentlichen
                                </Button>
                            }
                        </Box>
                    </>
                }
            </Box>

            <PublishPresetDialog
                preset={props.preset}
                presetVersion={props.entity}
                open={isNewModule && showPublishDialog}
                onClose={() => {
                    setShowPublishDialog(false);
                }}
                onPublish={handlePublishNewPreset}
            />

            <PublishPresetVersionDialog
                preset={props.preset}
                presetVersion={props.entity}
                open={isNewModuleVersion && showPublishDialog}
                onClose={() => {
                    setShowPublishDialog(false);
                }}
                onPublish={handlePublishNewVersion}
            />
        </>
    );
}

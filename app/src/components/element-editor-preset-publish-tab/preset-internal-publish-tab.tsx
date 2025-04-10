import {Box, Button, Typography} from '@mui/material';
import React from 'react';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import {type PresetVersion} from '../../models/entities/preset-version';
import {AlertComponent} from '../alert/alert-component';
import {Checklist} from '../checklist/checklist';
import {isElementTested} from '../../utils/is-element-tested';
import {hasUntestedChild} from '../../utils/has-untested-child';
import {type ChecklistItem} from '../checklist/checklist-props';
import {type TabProps} from './tab-props';
import {formatIsoDate} from '../../utils/time-utils';
import {useApi} from "../../hooks/use-api";
import {PresetVersionApiService} from "../../modules/presets/preset-version-api-service";

export function PresetInternalPublishTab(props: TabProps<GroupLayout, PresetVersion>): JSX.Element {
    const api = useApi();
    const presetVersionApiService = new PresetVersionApiService(api, props.preset.key);

    const checklist: ChecklistItem[] = [
        {
            label: 'Die Vorlage wurde technisch und fachlich geprüft',
            done: (
                isElementTested(props.element) &&
                !hasUntestedChild(props.element)
            ),
        },
    ];

    const handlePublish = (): void => {
        presetVersionApiService.update(props.entity.version, {
            ...props.entity,
            root: props.element,
            publishedAt: new Date().toISOString(),
        })
            .then((res) => {
                props.onChangeEntity(res);
                props.onPresetChange({
                    ...props.preset,
                    currentPublishedVersion: props.entity.version,
                });
                // TODO: Change saving logic to circumvent the need to hit the save button to update the main entity and element. Maybe via a flag to pass the direct save through.
                window.location.reload();
            })
            .catch((err) => {
                console.error(err);
            });
    };

    return (
        <>
            {
                props.entity.publishedAt == null &&
                <Box>
                    <AlertComponent
                        color="info"
                        title={`Version ${props.entity.version} ist unveröffentlicht`}
                    >
                        Version <strong>{props.entity.version}</strong> der Vorlage <strong>{props.preset.title}</strong> wurde noch nicht veröffentlicht. {
                            props.preset.currentPublishedVersion != null &&
                            <>Die aktuelle veröffentlichte Version ist <strong>{props.preset.currentPublishedVersion}</strong>.</>
                        }
                    </AlertComponent>

                    <Typography
                        variant="h6"
                        sx={{
                            mt: 4,
                        }}
                    >
                        Diese Version der Vorlage veröffentlichen
                    </Typography>

                    <Typography
                        variant="body1"
                    >
                        Bevor diese Version der Vorlage veröffentlicht werden kann, müssen die folgenden Punkte erfüllt sein.
                    </Typography>

                    <Checklist items={checklist}/>

                    <Box
                        sx={{
                            mt: 4,
                        }}
                    >
                        <Button
                            variant="contained"
                            onClick={handlePublish}
                            disabled={!checklist.every((item) => item.done)}
                        >
                            Version veröffentlichen
                        </Button>
                    </Box>
                </Box>
            }

            {

                props.entity.publishedAt != null &&
                <Box>
                    <AlertComponent
                        color="info"
                        title="Version veröffentlicht"
                    >
                        Die Version <strong>{props.entity.version}</strong> der Vorlage <strong>{props.preset.title}</strong> wurde am <strong>{formatIsoDate(props.entity.publishedAt)}</strong> veröffentlicht.
                    </AlertComponent>

                    {
                        props.preset.currentPublishedVersion !== props.entity.version &&
                        <AlertComponent
                            color="warning"
                            title="Neuere Version veröffentlicht"
                        >
                            Für die Vorlage <strong>{props.preset.title}</strong> wurde bereits eine neuere Version <strong>{props.preset.currentPublishedVersion}</strong> veröffentlicht.
                        </AlertComponent>
                    }
                </Box>
            }
        </>
    );
}

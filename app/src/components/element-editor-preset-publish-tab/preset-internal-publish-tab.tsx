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
import {useApi} from '../../hooks/use-api';
import {PresetVersionApiService} from '../../modules/presets/preset-version-api-service';
import {FormStatus} from '../../modules/forms/enums/form-status';

export function PresetInternalPublishTab(props: TabProps<GroupLayout, PresetVersion>) {
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
        presetVersionApiService
            .publish(props.entity.presetKey, props.entity.version)
            .then((res) => {
                props.onChangeEntity(res);
                props.onPresetChange({
                    ...props.preset,
                    publishedVersion: props.entity.version,
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
                props.entity.status == FormStatus.Drafted &&
                <Box>
                    <AlertComponent
                        color="info"
                        title={`Version ${props.entity.version} ist unveröffentlicht`}
                    >
                        Version <strong>{props.entity.version}</strong> der Vorlage <strong>{props.preset.title}</strong> wurde noch nicht veröffentlicht. {
                        props.preset.publishedVersion != null &&
                        <>Die aktuelle veröffentlichte Version ist <strong>{props.preset.publishedVersion}</strong>.</>
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

                    <Checklist items={checklist} />

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

                props.entity.status == FormStatus.Published &&
                props.entity.published != null &&
                <Box>
                    <AlertComponent
                        color="info"
                        title="Version veröffentlicht"
                    >
                        Die Version <strong>{props.entity.version}</strong> der Vorlage <strong>{props.preset.title}</strong> wurde am <strong>{formatIsoDate(props.entity.published)}</strong> veröffentlicht.
                    </AlertComponent>
                </Box>
            }

            {
                props.entity.status == FormStatus.Revoked &&
                props.entity.revoked != null &&
                <Box>
                    <AlertComponent
                        color="info"
                        title="Version zurückgezogen"
                    >
                        Die Version <strong>{props.entity.version}</strong> der Vorlage <strong>{props.preset.title}</strong> wurde am <strong>{formatIsoDate(props.entity.revoked)}</strong> zurückgezogen.
                    </AlertComponent>
                </Box>
            }
        </>
    );
}

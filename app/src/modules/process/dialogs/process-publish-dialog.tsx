import {DialogProps} from '@mui/material/Dialog';
import {useEffect, useState} from 'react';
import {ProcessNodeProblems} from '../entities/process-node-problems';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {Button, Dialog, DialogActions, DialogContent, Skeleton, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ProcessDefinitionVersionApiService} from '../services/process-definition-version-api-service';
import {Permission} from '../../../data/permissions/permission';
import {InsufficientPermissionAlert} from '../../../components/insufficient-permission-alert';
import type {ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {NodeProblemsAlert} from '../components/node-problems-alert';
import {AlertComponent} from '../../../components/alert/alert-component';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {isApiError} from '../../../models/api-error';
import {useConfirm} from '../../../providers/confirm-provider';
import {useCheckProcessPermission} from '../../permissions/hooks/use-permissions';

interface ProcessPublishDialogProps {
    process: ProcessEntity;
    version: ProcessVersionEntity;
    availableNodeProviders: ProcessNodeProvider[];
    onPublish: (publishedVersion: ProcessVersionEntity) => void;
    onClose: () => void;
}

export function ProcessPublishDialog(props: ProcessPublishDialogProps & DialogProps) {
    const {
        process,
        version,
        availableNodeProviders,
        onPublish,
        ...rest
    } = props;

    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const canPublish = useCheckProcessPermission(process.id, Permission.PROCESS_DEFINITION_PUBLISH_LOCAL);
    const replacesPublishedVersion = process.publishedVersion != null && process.publishedVersion !== version.processVersion;

    const [publishError, setPublishError] = useState<string>();
    useEffect(() => {
        setPublishError(undefined);
    }, [rest.open]);

    const [allNodeProblems, setAllNodeProblems] = useState<ProcessNodeProblems[]>([]);
    const [isValidating, setIsValidating] = useState<boolean>(false);
    const isReadyToPublish = canPublish && !isValidating && allNodeProblems.length === 0;

    useEffect(() => {
        setIsValidating(true);
        new ProcessDefinitionVersionApiService()
            .validate({
                processDefinitionId: process.id,
                processDefinitionVersion: version.processVersion,
            })
            .then(setAllNodeProblems)
            .finally(() => setIsValidating(false));
    }, [process.id, version.processVersion, rest.open]);

    const handlePublish = async () => {
        setPublishError(undefined);

        /*
        if (replacesPublishedVersion) {
            const confirmed = await showConfirm({
                title: 'Veröffentlichte Version ersetzen?',
                confirmButtonText: 'Ja, Version veröffentlichen',
                children: (
                    <Typography>
                        Aktuell ist Version {process.publishedVersion} veröffentlicht. Wenn Sie Version {version.processVersion} veröffentlichen,
                        wird Version {process.publishedVersion} zurückgezogen und Version {version.processVersion} veröffentlicht.
                    </Typography>
                ),
            });

            if (!confirmed) {
                return;
            }
        }
         */

        dispatch(setLoadingMessage({
            message: 'Prozess wird veröffentlicht',
            estimatedTime: 2000,
            blocking: true,
        }));

        new ProcessDefinitionVersionApiService()
            .publish({
                processDefinitionId: process.id,
                processDefinitionVersion: version.processVersion,
            })
            .then((publishedVersion) => {
                onPublish(publishedVersion);
            })
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    setPublishError(err.message);
                } else {
                    console.error(err);
                    setPublishError('Beim Veröffentlichen des Prozesses ist ein Fehler aufgetreten.');
                }
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    };

    return (
        <Dialog
            fullWidth={true}
            maxWidth="sm"
            {...rest}
        >
            <DialogTitleWithClose onClose={rest.onClose}>
                Prozess veröffentlichen
            </DialogTitleWithClose>
            <DialogContent>
                {
                    !canPublish &&
                    <InsufficientPermissionAlert
                        message="Sie verfügen nicht über die notwendige Berechtigung um diesen Prozess zu veröffentlichen."
                        requiredPermissions={[
                            Permission.PROCESS_DEFINITION_PUBLISH_LOCAL,
                        ]}
                    />
                }

                {
                    publishError != null &&
                    <AlertComponent
                        color="error"
                        title="Prozess konnte nicht veröffentlicht werden"
                        text={publishError}
                        sx={{mb: 2}}
                    />
                }

                {
                    isValidating &&
                    <Skeleton height={100}/>
                }

                {
                    !isValidating &&
                    allNodeProblems.length > 0 &&
                    <NodeProblemsAlert
                        problems={allNodeProblems}
                        availableNodeProviders={availableNodeProviders}
                        mode="publish"
                    />
                }

                {
                    isReadyToPublish &&
                    replacesPublishedVersion &&
                    <AlertComponent
                        color="warning"
                        title="Veröffentlichte Version wird ersetzt"
                    >
                        Aktuell ist Version {process.publishedVersion} veröffentlicht. Wenn Sie Version {version.processVersion} veröffentlichen,
                        wird Version {process.publishedVersion} zurückgezogen und Version {version.processVersion} veröffentlicht.
                    </AlertComponent>
                }

                {
                    isReadyToPublish &&
                    !replacesPublishedVersion &&
                    <AlertComponent
                        color="success"
                        title="Prozess bereit zur Veröffentlichung"
                    >
                        Sie können diesen Prozess veröffentlichen.
                    </AlertComponent>
                }
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    disabled={!canPublish || isValidating || allNodeProblems.length > 0}
                    onClick={handlePublish}
                >
                    Jetzt veröffentlichen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

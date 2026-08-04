import {DialogProps} from '@mui/material/Dialog';
import {useEffect, useState} from 'react';
import {ProcessNodeProblems} from '../entities/process-node-problems';
import {ProcessEntity} from '../entities/process-entity';
import {ProcessVersionEntity} from '../entities/process-version-entity';
import {Button, Dialog, DialogActions, DialogContent, Skeleton, Stack, Typography} from '@mui/material';
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
import {useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';

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
    const renderProcess = useRetainedDialogValue(rest.open, process);
    const renderVersion = useRetainedDialogValue(rest.open, version);
    const renderAvailableNodeProviders = useRetainedDialogValue(rest.open, availableNodeProviders);
    const renderOnPublish = useRetainedDialogValue(rest.open, onPublish);

    const canPublish = useHasProcessPermission(renderProcess.id, Permission.PROCESS_DEFINITION_PUBLISH_LOCAL);
    const replacesPublishedVersion = renderProcess.publishedVersion != null && renderProcess.publishedVersion !== renderVersion.processVersion;

    const [publishError, setPublishError] = useState<string>();
    const [validationError, setValidationError] = useState<string>();
    useEffect(() => {
        setPublishError(undefined);
        setValidationError(undefined);
    }, [rest.open]);

    const [allNodeProblems, setAllNodeProblems] = useState<ProcessNodeProblems[]>([]);
    const [isValidating, setIsValidating] = useState<boolean>(false);
    const isReadyToPublish = canPublish && !isValidating && validationError == null && allNodeProblems.length === 0;

    useEffect(() => {
        if (!rest.open) {
            return;
        }

        let isActive = true;

        setValidationError(undefined);
        setAllNodeProblems([]);
        setIsValidating(true);
        new ProcessDefinitionVersionApiService()
            .validate({
                processDefinitionId: renderProcess.id,
                processDefinitionVersion: renderVersion.processVersion,
            })
            .then((problems) => {
                if (isActive) {
                    setAllNodeProblems(problems);
                }
            })
            .catch((err) => {
                if (!isActive) {
                    return;
                }

                if (isApiError(err) && err.displayableToUser) {
                    setValidationError(err.message);
                } else {
                    console.error(err);
                    setValidationError('Die Prozessversion konnte nicht geprüft werden.');
                }
            })
            .finally(() => {
                if (isActive) {
                    setIsValidating(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [renderProcess.id, renderVersion.processVersion, rest.open]);

    const handlePublish = async () => {
        setPublishError(undefined);

        dispatch(setLoadingMessage({
            message: 'Prozessversion wird veröffentlicht',
            estimatedTime: 2000,
            blocking: true,
        }));

        new ProcessDefinitionVersionApiService()
            .publish({
                processDefinitionId: renderProcess.id,
                processDefinitionVersion: renderVersion.processVersion,
            })
            .then((publishedVersion) => {
                renderOnPublish(publishedVersion);
            })
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    setPublishError(err.message);
                } else {
                    console.error(err);
                    setPublishError('Beim Veröffentlichen der Prozessversion ist ein Fehler aufgetreten.');
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
                Prozessversion veröffentlichen
            </DialogTitleWithClose>
            <DialogContent>
                <Stack spacing={2}>
                    <Typography>
                        Sie veröffentlichen Version {renderVersion.processVersion} dieses Prozesses.
                        Nach der Veröffentlichung wird diese Version für neue Vorgänge verwendet.
                        Bestehende Vorgänge behalten die Prozessversion, mit der sie angelegt wurden.
                    </Typography>

                    {
                        !canPublish &&
                        <InsufficientPermissionAlert
                            message="Sie verfügen nicht über die notwendige Berechtigung um diese Prozessversion zu veröffentlichen."
                            requiredPermissions={[
                                Permission.PROCESS_DEFINITION_PUBLISH_LOCAL,
                            ]}
                        />
                    }

                    {
                        publishError != null &&
                        <AlertComponent
                            color="error"
                            title="Prozessversion konnte nicht veröffentlicht werden"
                            text={publishError}
                        />
                    }

                    {
                        validationError != null &&
                        <AlertComponent
                            color="error"
                            title="Prozessversion konnte nicht geprüft werden"
                            text={validationError}
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
                            availableNodeProviders={renderAvailableNodeProviders}
                            mode="publish"
                        />
                    }

                    {
                        isReadyToPublish &&
                        replacesPublishedVersion &&
                        <AlertComponent
                            color="warning"
                            title="Veröffentlichte Prozessversion wird zurückgezogen"
                        >
                            Aktuell ist Version {renderProcess.publishedVersion} veröffentlicht.
                            Wenn Sie Version {renderVersion.processVersion} veröffentlichen, wird Version {renderProcess.publishedVersion} zurückgezogen.
                        </AlertComponent>
                    }
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    disabled={!canPublish || isValidating || validationError != null || allNodeProblems.length > 0}
                    onClick={handlePublish}
                >
                    Prozessversion veröffentlichen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

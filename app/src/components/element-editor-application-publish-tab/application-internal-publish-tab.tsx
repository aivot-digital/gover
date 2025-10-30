import {type ElementEditorContentProps} from '../element-editor-content/element-editor-content-props';
import {Alert, AlertTitle, Box, Button, Divider, Paper, Skeleton, Tooltip} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {Form as Application} from '../../models/entities/form';
import {type RootElement} from '../../models/elements/root-element';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectMemberships} from '../../slices/user-slice';
import {UserRole} from '../../data/user-role';
import {Checklist} from '../checklist/checklist';
import PauseCircleOutlineOutlinedIcon from '@mui/icons-material/PauseCircleOutlineOutlined';
import {updateLoadedForm} from '../../slices/app-slice';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {AlertComponent} from '../alert/alert-component';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import {useApi} from '../../hooks/use-api';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {FormPublishChecklistItem} from '../../modules/forms/dtos/form-publish-checklist-item';
import {hideLoadingOverlay, showLoadingOverlay} from '../../slices/loading-overlay-slice';
import {useConfirm} from '../../providers/confirm-provider';
import {FormType} from '../../modules/forms/enums/form-type';
import {SxProps} from '@mui/material/styles';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {FormStatus} from '../../modules/forms/enums/form-status';
import {withDelay} from '../../utils/with-delay';

export function ApplicationInternalPublishTab<T extends RootElement, E extends Application>(props: ElementEditorContentProps<T, E>) {
    const api = useApi();
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();

    const [isLoading, setIsLoading] = useState(true);

    const [checklist, setChecklist] = useState<FormPublishChecklistItem[] | null>(null);
    const memberships = useAppSelector(selectMemberships);

    const [isPublished, setIsPublished] = useState(props.entity.status === FormStatus.Published);
    const [isRevoked, setIsRevoked] = useState(props.entity.status === FormStatus.Revoked);
    const [isIdentityRequired, setIsIdentityRequired] = useState(props.entity.identityVerificationRequired);
    const [isInternal, setIsInternal] = useState(props.entity.type === FormType.Internal);

    useEffect(() => {
        withDelay(new FormsApiService(api)
            .checkPublish({
                id: props.entity.id,
                version: props.entity.version,
            }), 600)
            .then(setChecklist)
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Veröffentlichungskriterien konnten nicht geladen werden.'));
            });
    }, [props.entity.id, api]);

    const allChecksDone = useMemo(() => {
        return checklist != null && checklist.every((item) => item.done);
    }, [checklist]);

    const renderInternalAlert = (sxProps?: SxProps<Object>) => {
        if (isInternal && isIdentityRequired) {
            return (
                <AlertComponent
                    color="info"
                    sx={sxProps}
                >
                    <AlertTitle>Internes Formular mit erzwungener Authentifizierung</AlertTitle>
                    <Box sx={{maxWidth: 940}}>
                        Dieses Formular erscheint nicht auf der öffentlichen Formularübersicht (Index-Liste), ist aber über den direkten Link zugänglich.
                        Ohne Authentifizierung sind die allgemeinen Informationen und Titel der Abschnitte öffentlich einsehbar.
                    </Box>
                </AlertComponent>
            );
        }

        if (isInternal) {
            return (
                <AlertComponent
                    color="info"
                    sx={sxProps}
                >
                    <AlertTitle>Internes Formular</AlertTitle>
                    <Box sx={{maxWidth: 940}}>
                        Dieses Formular erscheint nicht auf der öffentlichen Formularliste (Index-Liste), ist aber über den direkten Link zugänglich.
                        Alle Inhalte sind öffentlich einsehbar.
                    </Box>
                </AlertComponent>
            );
        }

        return null;
    };

    const canPublish = useMemo(() => {
        if (memberships == null) {
            return false;
        }

        return memberships
            .some((mem) => (
                    mem.departmentId === props.entity.developingDepartmentId &&
                    (mem.role === UserRole.Admin || mem.role === UserRole.Publisher)
                ),
            );
    }, [memberships]);

    const handlePublish = async (): Promise<void> => {
        const confirmed = await showConfirm({
            title: 'Formular veröffentlichen?',
            confirmButtonText: 'Ja, Formular veröffentlichen',
            children: (
                <>
                    <Box>
                        Möchten Sie dieses Formular wirklich veröffentlichen? Dieses Formular bzw. diese Version steht ab diesem Zeitpunkt online zur Verfügung und kann ausgefüllt werden.
                    </Box>
                    <Box sx={{mt: 2}}>
                        Wenn Sie das Formular veröffentlichen, können Sie die Inhalte dieser Version nicht mehr bearbeiten und müssen für Änderungen am Formular eine neue Version erstellen.
                    </Box>
                    {
                        isInternal && renderInternalAlert({mt: 4, mb: 0})
                    }
                </>
            ),
        });

        if (confirmed) {
            dispatch(showLoadingOverlay('Formular wird veröffentlicht'));

            new FormsApiService(api)
                .publish({
                    id: props.entity.id,
                    version: props.entity.version,
                })
                .then((updatedForm) => {
                    setIsPublished(true);
                    setIsRevoked(false);
                    dispatch(updateLoadedForm(updatedForm));
                })
                .catch((err) => {
                    if (err.status === 403) {
                        dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                    }
                    if (err.status === 406) {
                        dispatch(showErrorSnackbar('Das Formular kann nicht veröffentlicht werden, da es ein global deaktiviertes Nutzer- oder Unternehmenskonto verwendet.'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Das Formular konnte nicht gespeichert werden.'));
                    }
                })
                .finally(() => {
                    dispatch(hideLoadingOverlay());
                });
        }
    };

    const handleRevoke = async (): Promise<void> => {
        const confirmed = await showConfirm({
            title: 'Formular zurückziehen?',
            confirmButtonText: 'Ja, Formular zurückziehen',
            isDestructive: true,
            children: (
                <div>
                    Möchten Sie dieses Formular wirklich zurückziehen? Dieses Formular bzw. diese Version ist ab diesem Zeitpunkt nicht mehr online verfügbar und kann nicht mehr ausgefüllt werden.
                </div>
            ),
        });

        if (confirmed) {
            dispatch(showLoadingOverlay('Formular wird zurückgezogen'));

            new FormsApiService(api)
                .revoke({
                    id: props.entity.id,
                    version: props.entity.version,
                })
                .then((updatedForm) => {
                    setIsRevoked(true);
                    setIsPublished(false);
                    dispatch(updateLoadedForm(updatedForm));
                })
                .catch((err) => {
                    if (err.status === 403) {
                        dispatch(showErrorSnackbar('Sie verfügen nicht über die notwendigen Rechte zum Bearbeiten.'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Das Formular konnte nicht zurückgezogen werden.'));
                    }
                })
                .finally(() => {
                    dispatch(hideLoadingOverlay());
                });
        }
    };

    return (
        <>
            <ElementEditorSectionHeader
                title="Formular veröffentlichen"
                disableMarginTop
                sx={{mb: 3}}
            >
                Bevor ein Formular veröffentlicht werden kann, müssen die folgenden Punkte erfüllt sein.
            </ElementEditorSectionHeader>

            <Box
                sx={{
                    my: 2,
                }}
            >
                <Paper variant="outlined">
                    {
                        checklist == null &&
                        <Box sx={{
                            px: 2,
                            py: 1,
                        }}>
                            {
                                new Array(8).fill(null).map((_, idx) => (
                                    <Skeleton
                                        width="100%"
                                        height={50}
                                    />
                                ))
                            }
                        </Box>
                    }
                    {
                        checklist != null &&
                        <Checklist
                            items={checklist}
                            sx={{
                                opacity: props.hasChanges ? 0.5 : 1,
                            }}
                        />
                    }
                </Paper>
            </Box>

            {
                isPublished &&
                <>
                    <Alert
                        severity="success"
                    >
                        Formular veröffentlicht
                    </Alert>
                </>
            }

            {
                isPublished && renderInternalAlert()
            }

            {
                isPublished &&
                canPublish &&
                <>
                    <Divider
                        sx={{my: 8}}
                    >
                        Formular zurückziehen
                    </Divider>

                    <Alert
                        severity="warning"
                        sx={{mb: 2}}
                    >
                        <AlertTitle>
                            Formular zurückziehen
                        </AlertTitle>

                        Sie können ein veröffentlichtes Formular jederzeit zurückziehen.
                        Dieses steht dann nicht mehr öffentlich zur Verfügung.
                        Sie können zurückgezogene Formulare jederzeit wieder veröffentlichen.
                    </Alert>

                    <Tooltip
                        title="Jetzt zurückziehen"
                    >
                        <Button
                            variant="outlined"
                            endIcon={
                                <PauseCircleOutlineOutlinedIcon />
                            }
                            color="warning"
                            onClick={handleRevoke}
                        >
                            Formular zurückziehen
                        </Button>
                    </Tooltip>
                </>
            }

            {
                isRevoked &&
                <Alert
                    severity="warning"
                    sx={{
                        mb: 2,
                    }}
                >
                    <AlertTitle>
                        Formular zurückgezogen
                    </AlertTitle>

                    Sie haben das Formular zurückgezogen.
                    Sie können dieses Formular jederzeit wieder veröffentlichen.
                </Alert>
            }

            {
                !isPublished &&
                !allChecksDone &&
                !props.hasChanges &&
                <AlertComponent color="info">
                    Vor dem Veröffentlichen müssen alle Kriterien erfüllt sein und Sie müssen die notwendigen
                    Berechtigungen haben.
                </AlertComponent>
            }

            {
                !isPublished &&
                allChecksDone &&
                !canPublish &&
                !props.hasChanges &&
                <AlertComponent color="info">
                    Sie verfügen nicht über die notwendigen Berechtigungen, um ein Formular in diesem Fachbereich zu
                    veröffentlichen.
                </AlertComponent>
            }

            {
                !isPublished &&
                props.hasChanges &&
                <AlertComponent color="warning">
                    Sie haben Änderungen am Formular vorgenommen. Bitte speichern Sie diese Änderungen, bevor Sie das
                    Formular veröffentlichen.
                </AlertComponent>
            }

            {
                !isPublished &&
                allChecksDone &&
                canPublish &&
                !props.hasChanges &&
                <Tooltip
                    title="Jetzt veröffentlichen"
                >
                    <Button
                        variant="contained"
                        endIcon={
                            <SendOutlinedIcon />
                        }
                        onClick={handlePublish}
                    >
                        Formular veröffentlichen
                    </Button>
                </Tooltip>
            }
        </>
    );
}

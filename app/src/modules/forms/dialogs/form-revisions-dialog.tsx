import React, {useEffect, useState} from 'react';
import {Box, Button, Card, Dialog, DialogContent, DialogContentText, Grid, Skeleton, Typography} from '@mui/material';
import {User} from '../../users/models/user';
import {DiffItem} from '../../../models/entities/form-revision';
import {AnyElement, AnyElementType} from '../../../models/elements/any-element';
import {DeletedElementReference, isDeletedElementReference, resolveElementPath} from '../../../utils/resolve-element-path';
import {Api, useApi} from '../../../hooks/use-api';
import {Page} from '../../../models/dtos/page';
import {UsersApiService} from '../../users/users-api-service';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {parseISO} from 'date-fns/parseISO';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {clearLoadedFormHistory, isLoadedForm, LoadedForm, selectLoadedForm, updateLoadedForm} from '../../../slices/app-slice';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../slices/loading-overlay-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {delayPromise} from '../../../utils/with-delay';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {getFullName} from '../../../models/entities/user';
import {format} from 'date-fns/format';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {RestoreOutlined} from '@mui/icons-material';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {FormStatus} from '../enums/form-status';
import {ElementType} from '../../../data/element-type/element-type';
import {FormApiService} from '../services/form-api-service';
import {FormVersionEntity, isFormVersionEntity} from '../entities/form-version-entity';


export interface FormRevisionsDialogProps {
    open: boolean;
    onClose: () => void;
}


interface Revision {
    id: number;
    timestamp: Date;
    user: User | undefined;
    diffs: RevisionDiff[];
}

interface RevisionDiff extends DiffItem {
    elementPath: (FormVersionEntity | AnyElement | DeletedElementReference)[];
    element: (FormVersionEntity | AnyElement | DeletedElementReference);
    title: string;
    path: string;
    field: string;
}

async function fetchRevisions(form: LoadedForm, lastPage: Page<Revision> | undefined): Promise<Page<Revision>> {
    const revisionsPage = await new FormApiService().listRevisions({
        formId: form.form.id,
        version: form.version.version,
    }, {
        queryParams: {
            limit: 10,
            page: lastPage == null ? 0 : (lastPage.number + 1),
        },
    });

    const userIdSet = revisionsPage
        .content
        .map(rev => rev.userId)
        .reduce((set, id) => {
            if (!set.includes(id)) {
                set.push(id);
            }
            return set;
        }, [] as string[]);

    const users: Record<string, User | undefined> = {};
    for (const uid of userIdSet) {
        try {
            users[uid] = await new UsersApiService().retrieve(uid);
        } catch (err) {
            console.error(err);
            users[uid] = undefined;
        }
    }

    const items: Revision[] = [];

    const resolveElementLabel = (e: FormVersionEntity | AnyElement | DeletedElementReference) => {
        if (isDeletedElementReference(e)) {
            return `Gelöschtes Element (Index: ${e.id})`;
        } else if (isFormVersionEntity(e)) {
            return 'Formular';
        } else {
            return generateComponentTitle(e);
        }
    };

    for (const rev of revisionsPage.content) {
        const diffs = [];
        for (const diff of rev.diff) {
            const elementPath = resolveElementPath(form.version, diff.field);
            const element = elementPath[elementPath.length - 1];
            const revDiff = {
                elementPath: elementPath,
                element: element,
                title: resolveElementLabel(element),
                path: elementPath.slice(1).map(e => resolveElementLabel(e)).join(' → '),
                ...diff,
                field: diff.field.split('/').pop() ?? '',
            };
            diffs.push(revDiff);
        }
        items.push({
            id: rev.id,
            timestamp: parseISO(rev.timestamp),
            user: users[rev.userId],
            diffs: diffs,
        });
    }

    return {
        ...revisionsPage,
        content: items,
    };
}

export function FormRevisionsDialog(props: FormRevisionsDialogProps) {
    const dispatch = useAppDispatch();
    const api = useApi();

    const form = useAppSelector(selectLoadedForm);

    const [isLoadingRevisions, setIsLoadingRevisions] = useState(false);
    const [revisions, setRevisions] = useState<Revision[]>([]);
    const [currentRevisionsPage, setCurrentRevisionsPage] = useState<Page<Revision>>();
    const [handleRollback, setHandleRollback] = useState<() => void>();

    const reset = () => {
        setRevisions([]);
        setIsLoadingRevisions(false);
        setCurrentRevisionsPage(undefined);
    };

    const performRollback = (id: number) => {
        if (form == null) {
            return;
        }

        dispatch(showLoadingOverlay('Änderung wird rückgängig gemacht…'));
        new FormApiService()
            .rollbackRevision({
                formId: form.form.id,
                version: form.version.version,
            }, id)
            .then((rolledBack) => {
                dispatch(showSuccessSnackbar('Änderung rückgängig gemacht.'));
                dispatch(updateLoadedForm({
                    ...form,
                    version: rolledBack,
                }));
                dispatch(clearLoadedFormHistory());
                setHandleRollback(undefined);
                props.onClose();
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Änderung konnte nicht rückgängig gemacht werden.'));
            })
            .finally(() => {
                dispatch(hideLoadingOverlay());
            });
    };

    useEffect(() => {
        if (props.open && form != null && api != null) {
            setIsLoadingRevisions(true);
            delayPromise(fetchRevisions(form, undefined), 1000)
                .then((revisionsPage) => {
                    setRevisions(revisionsPage.content);
                    setCurrentRevisionsPage(revisionsPage);
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Die Historie des Formulars konnte nicht geladen werden.'));
                })
                .finally(() => {
                    setIsLoadingRevisions(false);
                });
        } else {
            reset();
        }
    }, [props.open]);

    const loadMoreRevisions = () => {
        if (props.open && form != null && api != null) {
            setIsLoadingRevisions(true);
            fetchRevisions(form, currentRevisionsPage)
                .then((revisionsPage) => {
                    setRevisions([
                        ...revisions,
                        ...revisionsPage.content,
                    ]);
                    setCurrentRevisionsPage(revisionsPage);
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Die Historie des Formulars konnte nicht geladen werden.'));
                })
                .finally(() => {
                    setIsLoadingRevisions(false);
                });
        }
    };

    return (
        <>
            <Dialog
                open={props.open}
                onClose={props.onClose}
                fullWidth
                maxWidth={'xl'}
            >

                <DialogTitleWithClose
                    onClose={props.onClose}
                    closeTooltip="Schließen"
                >
                    {
                        form == null &&
                        <Skeleton width="100%" />
                    }
                    {
                        form != null &&
                        `Historie für das Formular „${form.form.internalTitle}“`
                    }
                </DialogTitleWithClose>

                <DialogContent tabIndex={0}>
                    {
                        !isLoadingRevisions &&
                        revisions.length == 0 &&
                        <Typography
                            variant="body2"
                        >
                            Es sind keine Änderungen vorhanden.
                        </Typography>
                    }

                    {
                        revisions.length > 0 &&
                        <>
                            <Typography
                                variant="body2"
                                sx={{
                                    mb: 3,
                                }}
                            >
                                Die Historie zeigt alle Änderungen am Formular.
                                Sie können jede Änderung rückgängig machen, auch die von anderen Mitarbeiter:innen.
                            </Typography>

                            <Typography
                                variant="h6"
                                sx={{
                                    mb: 2,
                                }}
                            >
                                Liste der Änderungen (neuste zuerst)
                            </Typography>

                            {
                                revisions &&
                                revisions.map((rev, index) => (
                                    <Card
                                        key={rev.id}
                                        sx={{
                                            mb: 2,
                                            p: 2,
                                        }}
                                        variant={'outlined'}
                                    >
                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                            }}
                                        >
                                            <Typography
                                                variant="h6"
                                                sx={{
                                                    marginBottom: 0.5,
                                                }}
                                            >
                                                Änderung{rev.diffs != null && rev.diffs.length > 1 ? 'en' : ''} von {getFullName(rev.user)} <Typography
                                                component={'span'}
                                                sx={{color: 'text.secondary'}}
                                            >(am {format(rev.timestamp, 'dd.MM.yyyy')} um {format(rev.timestamp, 'HH:mm:ss')} Uhr)</Typography>
                                            </Typography>
                                        </Box>

                                        {
                                            rev.diffs.map((diff: RevisionDiff, i: number) => (
                                                <Box
                                                    key={diff.path + diff.field}
                                                    sx={{
                                                        marginBottom: 3,
                                                    }}
                                                >

                                                    {diff.field &&
                                                        <Typography>
                                                            Geändertes Attribut: {diff.path}{diff.path && ' → '}
                                                            <strong>{getFieldLabel(diff)}</strong>
                                                        </Typography>
                                                    }

                                                    <Grid
                                                        container
                                                        key={diff.field}
                                                        spacing={2}
                                                        sx={{mt: -1}}
                                                    >
                                                        <Grid
                                                            size={{
                                                                xs: 12,
                                                                md: 6,
                                                            }}
                                                        >
                                                            <Typography
                                                                variant="caption"
                                                                sx={{
                                                                    display: 'block',
                                                                }}
                                                            >
                                                                Alter Wert
                                                            </Typography>

                                                            <ExpandableCodeBlock value={JSON.stringify(diff.oldValue, null, '\t')} />
                                                        </Grid>

                                                        <Grid
                                                            size={{
                                                                xs: 12,
                                                                md: 6,
                                                            }}
                                                        >
                                                            <Typography
                                                                variant="caption"
                                                                sx={{
                                                                    display: 'block',
                                                                }}
                                                            >
                                                                Neuer Wert
                                                            </Typography>

                                                            <ExpandableCodeBlock value={JSON.stringify(diff.newValue, null, '\t')} />
                                                        </Grid>
                                                    </Grid>
                                                </Box>
                                            ))
                                        }

                                        {
                                            (currentRevisionsPage?.last !== true ||
                                                index < revisions.length - 1) &&
                                            (form?.version.status === FormStatus.Drafted) &&
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                }}
                                            >
                                                <Button
                                                    variant="contained"
                                                    sx={{
                                                        ml: 'auto',
                                                    }}
                                                    size="small"
                                                    onClick={() => setHandleRollback(() => () => performRollback(rev.id))}
                                                    startIcon={<RestoreOutlined />}
                                                >
                                                    Diese Änderung{rev.diffs != null && rev.diffs.length > 1 ? 'en' : ''} rückgängig machen
                                                </Button>
                                            </Box>
                                        }
                                    </Card>
                                ))
                            }
                        </>
                    }

                    {
                        currentRevisionsPage != null &&
                        currentRevisionsPage.last === false &&
                        isLoadingRevisions === false &&
                        <Button
                            variant="contained"
                            fullWidth
                            onClick={loadMoreRevisions}
                            sx={{
                                mt: 4,
                            }}
                        >
                            Weitere Änderungen laden
                        </Button>
                    }

                    {
                        isLoadingRevisions &&
                        <Box sx={{p: 4, pb: 6}}>
                            <LoadingPlaceholder
                                message="Historie wird geladen"
                            />
                        </Box>
                    }
                </DialogContent>
            </Dialog>
            <ConfirmDialog
                title="Änderung rückgängig machen"
                onCancel={() => setHandleRollback(undefined)}
                onConfirm={handleRollback}
            >
                <DialogContentText>
                    Wenn Sie diese Änderung rückgängig machen, werden auch alle nachfolgenden Änderungen, die zeitlich nach der rückgängig zu machenden Änderung erfolgt sind, ebenfalls zurückgesetzt.
                </DialogContentText>
                <DialogContentText
                    sx={{
                        mt: 2,
                    }}
                >
                    Das Rückgängigmachen kann einige Zeit in Anspruch nehmen. Bitte schließen Sie den Editor während dieses Vorgangs nicht. Bestätigen Sie bitte das Rückgängigmachen der Änderung, um fortzufahren.
                </DialogContentText>
                <DialogContentText
                    sx={{
                        mt: 2,
                    }}
                >
                    <b>Wichtig:</b>
                    Beim Rückgängigmachen wird geprüft, ob bestimmte Attribute noch gültig oder verfügbar sind. Sollte dies nicht der Fall sein, werden diese entfernt und müssen anschließend neu gesetzt werden. Betroffen sein können
                    insbesondere: Formular-IDs, Schnittstellen, Statusangaben, Fachbereiche, rechtliche Hinweise, Support-Informationen, PDF-Vorlagen, Farbschemata, Zahlungsdetails sowie Testprotokolle.
                </DialogContentText>
            </ConfirmDialog>
        </>
    );
}

function getFieldLabel(diff: RevisionDiff): string {
    const elem = diff.element;

    if (isFormVersionEntity(elem)) {
        return formFieldLabelMap[diff.field as keyof FormVersionEntity] ?? diff.field;
    }

    if (isDeletedElementReference(diff)) {
        return 'Gelöschtes Element';
    }

    const elementType = (elem as AnyElement).type as ElementType;
    const fieldLabelMap = et[elementType];

    if (fieldLabelMap != null) {
        const label = fieldLabelMap[diff.field as keyof AnyElementType<ElementType>];
        if (label != null) {
            return label;
        }
    }

    return diff.field;
}

const formFieldLabelMap: Partial<Record<keyof FormVersionEntity, string>> = {
    publicTitle: 'Öffentlicher Titel',
    type: 'Formular-Typ',
    responsibleDepartmentId: 'Verantwortlicher Fachbereich',
    managingDepartmentId: 'Bewirtschaftender Fachbereich',
    accessibilityDepartmentId: 'Text für die Erklärung der Barrierefreiheit',
    privacyDepartmentId: 'Text für die Datenschutzerklärung',
    imprintDepartmentId: 'Text für das Impressum',
    customerAccessHours: 'Zugriffsfrist in Stunden',
    submissionRetentionWeeks: 'Löschfrist in Wochen',
};

// Map element types to records of
type ElementFieldLabelMap = {
    [T in ElementType]?: Partial<Record<keyof AnyElementType<T>, string>>;
};

const et: ElementFieldLabelMap = {
    [ElementType.Root]: {
        headline: 'Überschrift',
        tabTitle: 'Tab-Titel',
        expiring: 'Ablaufdatum',
    },
};
import React, {useEffect, useState} from 'react';
import {Box, Button, Dialog, DialogContent, Grid, Skeleton, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type FormRevisionsDialogProps} from './form-revisions-dialog-props';
import {Api, useApi} from '../../hooks/use-api';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useFormsApi} from '../../hooks/use-forms-api';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {DiffItem} from '../../models/entities/form-revision';
import {useAppSelector} from '../../hooks/use-app-selector';
import {clearLoadedFormHistory, selectLoadedForm, updateLoadedForm} from '../../slices/app-slice';
import {getFullName, User} from '../../models/entities/user';
import {useUsersApi} from '../../hooks/use-users-api';
import {Form, isForm} from '../../models/entities/form';
import {AnyElement} from '../../models/elements/any-element';
import {format, parseISO} from 'date-fns';
import {DeletedElementReference, isDeletedElementReference, resolveElementPath} from '../../utils/resolve-element-path';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {delayPromise} from '../../utils/with-delay';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';
import {ConfirmDialog} from '../confirm-dialog/confirm-dialog';
import {hideLoadingOverlay, showLoadingOverlay} from '../../slices/loading-overlay-slice';
import {Page} from '../../models/dtos/page';
import {FormsApiService} from '../../services/forms-api-service';
import {ApplicationStatus} from '../../data/application-status';

interface Revision {
    id: number;
    timestamp: Date;
    user: User | undefined;
    diffs: RevisionDiff[];
}

interface RevisionDiff extends DiffItem {
    elementPath: (Form | AnyElement | DeletedElementReference)[];
    element: (Form | AnyElement | DeletedElementReference);
    title: string;
    path: string;
    field: string;
}

async function fetchRevisions(form: Form, api: Api, lastPage: Page<Revision> | undefined): Promise<Page<Revision>> {
    const formsApiService = new FormsApiService(api);

    const revisionsPage = await formsApiService.listRevisions(form.id, {
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
            users[uid] = await useUsersApi(api).retrieve(uid);
        } catch (err) {
            console.error(err);
            users[uid] = undefined;
        }
    }

    const items: Revision[] = [];

    const resolveElementLabel = (e: Form | AnyElement | DeletedElementReference) => {
        if (isDeletedElementReference(e)) {
            return `Gelöschtes Element (Index: ${e.id})`;
        } else if (isForm(e)) {
            return e.title;
        } else {
            return generateComponentTitle(e);
        }
    };

    for (const rev of revisionsPage.content) {
        const diffs = [];
        for (const diff of rev.diff) {
            const elementPath = resolveElementPath(form, diff.field);
            const element = elementPath[elementPath.length - 1];
            const revDiff = {
                elementPath: elementPath,
                element: element,
                title: resolveElementLabel(element),
                path: elementPath.map(e => resolveElementLabel(e)).join(' > '),
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

export function FormRevisionsDialog(props: FormRevisionsDialogProps): JSX.Element {
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

        dispatch(showLoadingOverlay('Änderung wird rückgängig gemacht...'));
        useFormsApi(api)
            .rollbackRevision(form.id, id)
            .then(form => {
                dispatch(showSuccessSnackbar('Änderung rückgängig gemacht.'));
                dispatch(updateLoadedForm(form));
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
            delayPromise(fetchRevisions(form, api, undefined), 1000)
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
            fetchRevisions(form, api, currentRevisionsPage)
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
                maxWidth="xl"
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
                        `Historie für ${form.title}`
                    }
                </DialogTitleWithClose>

                <DialogContent tabIndex={0}>
                    {
                        revisions.length > 0 &&
                        <>
                            <Typography
                                variant="body2"
                                sx={{
                                    mb: 2,
                                }}
                            >
                                Die Historie zeigt alle Änderungen am Formular.
                                Sie können jede Änderung rückgängig machen, auch die von anderen Mitarbeiter:innen.
                                Da die Historie auf dem Server gespeichert wird, kann es sein, dass Ihre lokale Version von der auf dem Server abweicht.
                                Beim Verlassen des Editors werden beide Versionen automatisch abgeglichen.
                            </Typography>

                            {
                                revisions &&
                                revisions.map((rev, index) => (
                                    <Box
                                        key={rev.id}
                                        mb={2}
                                        pb={2}
                                        borderBottom={index < revisions.length - 1 ? 1 : 0}
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
                                                {getFullName(rev.user)} @ {format(rev.timestamp, 'dd.MM.yyyy HH:mm:ss')} Uhr
                                            </Typography>
                                        </Box>

                                        {
                                            rev.diffs.map((diff, i) => (
                                                <Box
                                                    key={diff.path + diff.field}
                                                    sx={{
                                                        padding: 1,
                                                        marginBottom: 1,
                                                        marginLeft: 1,
                                                    }}
                                                >

                                                    <Typography variant="caption">
                                                        {diff.path} &gt; {diff.field}
                                                    </Typography>

                                                    <Grid
                                                        container
                                                        key={diff.field}
                                                        spacing={2}
                                                    >
                                                        <Grid
                                                            item
                                                            xs={12}
                                                            md={6}
                                                        >
                                                            <Typography
                                                                variant="caption"
                                                                sx={{
                                                                    display: 'block',
                                                                }}
                                                            >
                                                                Alter Wert
                                                            </Typography>

                                                            <Typography
                                                                component="code"
                                                                sx={{
                                                                    display: 'block',
                                                                    whiteSpace: 'break-spaces',
                                                                    width: '100%',
                                                                    overflowX: 'auto',
                                                                    padding: 1,
                                                                    border: '1px solid gray',
                                                                    backgroundColor: '#fafafa',
                                                                }}
                                                            >
                                                                {JSON.stringify(diff.oldValue, null, '\t')}
                                                            </Typography>
                                                        </Grid>

                                                        <Grid
                                                            item
                                                            xs={12}
                                                            md={6}
                                                        >
                                                            <Typography
                                                                variant="caption"
                                                                sx={{
                                                                    display: 'block',
                                                                }}
                                                            >
                                                                Neuer Wert
                                                            </Typography>

                                                            <Typography
                                                                component="code"
                                                                sx={{
                                                                    display: 'block',
                                                                    whiteSpace: 'break-spaces',
                                                                    width: '100%',
                                                                    overflowX: 'auto',
                                                                    padding: 1,
                                                                    border: '1px solid gray',
                                                                    backgroundColor: '#fafafa',
                                                                }}
                                                            >
                                                                {JSON.stringify(diff.newValue, null, '\t')}
                                                            </Typography>
                                                        </Grid>
                                                    </Grid>
                                                </Box>
                                            ))
                                        }

                                        {
                                            (currentRevisionsPage?.last !== true ||
                                                index < revisions.length - 1) &&
                                            (form?.status === ApplicationStatus.Drafted ||
                                                form?.status === ApplicationStatus.InReview) &&
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                }}
                                            >
                                                <Button
                                                    variant="outlined"
                                                    sx={{
                                                        mb: 2,
                                                        ml: 'auto',
                                                    }}
                                                    size="small"
                                                    onClick={() => setHandleRollback(() => () => performRollback(rev.id))}
                                                >
                                                    Diese Änderung{rev.diffs != null && rev.diffs.length > 1 ? 'en' : ''} rückgängig machen
                                                </Button>
                                            </Box>
                                        }
                                    </Box>
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
                            Mehr Änderungen laden
                        </Button>
                    }

                    {
                        isLoadingRevisions &&
                        <LoadingPlaceholder
                            message="Lade Historie..."
                        />
                    }
                </DialogContent>
            </Dialog>

            <ConfirmDialog
                title="Änderung rückgängig machen"
                onCancel={() => setHandleRollback(undefined)}
                onConfirm={handleRollback}
            >
                <Typography>
                    Wenn Sie diese Änderung rückgängig machen, werden auch alle Änderungen rückgängig gemacht, welche zeitlich nach der Änderung (welche rückgängig gemacht werden soll) erfolgt sind.
                </Typography>
                <Typography
                    sx={{
                        mt: 1,
                    }}
                >
                    Das Rückgängig machen kann etwas Zeit in Anspruch nehmen.
                    Bitte schließen Sie den Editor nicht, während der Vorgang läuft.
                    Bitte bestätigen Sie das Rückgängig machen der Änderung um fortzufahren.
                </Typography>
            </ConfirmDialog>
        </>
    );
}

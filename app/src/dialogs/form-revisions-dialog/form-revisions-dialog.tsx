import React, {useEffect, useState} from 'react';
import {Box, Dialog, DialogContent, Skeleton, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type FormRevisionsDialogProps} from './form-revisions-dialog-props';
import {Api, useApi} from '../../hooks/use-api';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {useFormsApi} from '../../hooks/use-forms-api';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {DiffItem} from '../../models/entities/form-revision';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {getFullName, User} from '../../models/entities/user';
import {useUsersApi} from '../../hooks/use-users-api';
import {Form, isForm} from '../../models/entities/form';
import {AnyElement} from '../../models/elements/any-element';
import {format, parseISO} from 'date-fns';
import {resolveElementPath} from '../../utils/resolve-element-path';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {delayPromise} from '../../utils/with-delay';
import {LoadingPlaceholder} from '../../components/loading-placeholder/loading-placeholder';

interface Revision {
    timestamp: Date;
    user: User | undefined;
    elementPath: (Form | AnyElement | 'deleted_element')[];
    element: (Form | AnyElement | 'deleted_element');
    title: string;
    path: string;
    field: string;
    diff: DiffItem;
}

async function fetchRevisions(form: Form, api: Api): Promise<Revision[]> {
    const revisions = await useFormsApi(api).listRevisions(form.id);

    const userIdSet = revisions
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

    const resolveElementLabel = (e: Form | AnyElement | 'deleted_element') => {
        if (e === 'deleted_element') {
            return 'Gelöschtes Element';
        } else if (isForm(e)) {
            return e.title;
        } else {
            return generateComponentTitle(e);
        }
    };

    for (const rev of revisions) {
        for (const diff of rev.diff) {
            const elementPath = resolveElementPath(form, diff.field);
            const element = elementPath[elementPath.length - 1];
            items.push({
                timestamp: parseISO(rev.timestamp),
                user: users[rev.userId],
                elementPath: elementPath,
                element: element,
                title: resolveElementLabel(element),
                path: elementPath.map(e => resolveElementLabel(e)).join(' > '),
                field: diff.field.split('/').pop() ?? '',
                diff: diff,
            });
        }
    }

    return items;
}

export function FormRevisionsDialog(props: FormRevisionsDialogProps): JSX.Element {
    const dispatch = useAppDispatch();
    const api = useApi();

    const form = useAppSelector(selectLoadedForm);

    const [revisions, setRevisions] = useState<Revision[]>([]);

    useEffect(() => {
        if (props.open && form != null && api != null) {
            delayPromise(fetchRevisions(form, api), 1000)
                .then(setRevisions)
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Die Historie des Formulars konnte nicht geladen werden.'));
                });
        } else {
            setRevisions([]);
        }
    }, [props.open]);

    return (
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
                    revisions.length == 0 &&
                    <LoadingPlaceholder
                        message="Lade Historie..."
                    />
                }
                {
                    revisions.length > 0 &&
                    <>
                        {
                            revisions &&
                            revisions.map(rev => (
                                <Box
                                    key={rev.path + rev.field + rev.timestamp.toISOString()}
                                    mb={2}
                                    pb={2}
                                    borderBottom={1}
                                >
                                    <Typography variant="caption">
                                        {getFullName(rev.user)} @ {format(rev.timestamp, 'dd.MM.yyyy hh:mm')} Uhr
                                    </Typography>

                                    <Typography variant="h6">
                                        {rev.path} &gt; {rev.field}
                                    </Typography>

                                    <Typography
                                        variant="caption"
                                        sx={{
                                            mt: 1,
                                            display: 'block',
                                        }}
                                    >
                                        Alter Wert
                                    </Typography>

                                    <Typography
                                        component="code"
                                        sx={{
                                            display: 'block',
                                            whiteSpace: 'nowrap',
                                            width: '100%',
                                            overflowX: 'auto',
                                            padding: 1,
                                            mt: 1,
                                            border: '1px solid gray',
                                            backgroundColor: '#fafafa',
                                        }}
                                    >
                                        {JSON.stringify(rev.diff.oldValue)}
                                    </Typography>

                                    <Typography
                                        variant="caption"
                                        sx={{
                                            mt: 2,
                                            display: 'block',
                                        }}
                                    >
                                        Neuer Wert
                                    </Typography>

                                    <Typography
                                        component="code"
                                        sx={{
                                            display: 'block',
                                            whiteSpace: 'nowrap',
                                            width: '100%',
                                            overflowX: 'auto',
                                            padding: 1,
                                            mt: 1,
                                            border: '1px solid gray',
                                            backgroundColor: '#fafafa',
                                        }}
                                    >
                                        {JSON.stringify(rev.diff.newValue)}
                                    </Typography>
                                </Box>
                            ))
                        }
                    </>
                }
            </DialogContent>
        </Dialog>
    );
}

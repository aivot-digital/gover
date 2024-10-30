import React, {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {type ProviderLink} from '../../../models/entities/provider-link';
import {useChangeBlocker} from '../../../hooks/use-change-blocker';
import {FormPageWrapper} from '../../../components/form-page-wrapper/form-page-wrapper';
import {delayPromise} from '../../../utils/with-delay';
import {useAdminGuard} from '../../../hooks/use-admin-guard';
import {ConfirmDialog} from '../../../dialogs/confirm-dialog/confirm-dialog';
import {useApi} from '../../../hooks/use-api';
import {useProviderLinksApi} from '../../../hooks/use-provider-links-api';

export function ProviderLinkEditPage(): JSX.Element {
    useAdminGuard();

    const api = useApi();
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const linkId = useParams().id;

    const [originalLink, setOriginalLink] = useState<ProviderLink>();
    const [editedLink, setEditedLink] = useState<ProviderLink>();

    const [isLoading, setIsLoading] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);

    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const hasChanged = useChangeBlocker(originalLink, editedLink);

    useEffect(() => {
        if (linkId == null || linkId === 'new') {
            const newDest: ProviderLink = {
                id: 0,
                text: '',
                link: '',
                created: new Date().toISOString(),
                updated: new Date().toISOString(),
            };
            setOriginalLink(newDest);
            setEditedLink(newDest);
        } else {
            setIsLoading(true);
            delayPromise(useProviderLinksApi(api).retrieveProviderLink(parseInt(linkId)))
                .then((link) => {
                    setOriginalLink(link);
                    setEditedLink(link);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    }, [linkId]);

    const handlePatch = (patch: Partial<ProviderLink>): void => {
        if (editedLink != null) {
            setEditedLink({
                ...editedLink,
                ...patch,
            });
        }
    };

    const handleSave = (): void => {
        if (editedLink == null) {
            return;
        }

        setIsLoading(true);
        useProviderLinksApi(api)
            .saveProviderLink(
                editedLink,
            )
            .then((responseLink) => {
                setOriginalLink(responseLink);
                setEditedLink(responseLink);
                dispatch(showSuccessSnackbar('Link erfolgreich gespeichert'));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Link konnte nicht gespeichert werden'));
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    const handleReset = (): void => {
        if (originalLink == null) {
            return;
        }
        setEditedLink(originalLink);
    };

    const handleDelete = (): void => {
        if (editedLink == null || editedLink.id === 0) {
            return;
        }

        setIsLoading(true);
        useProviderLinksApi(api)
            .destroyProviderLink(editedLink.id)
            .then(() => {
                navigate('/provider-links');
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Link konnte nicht gelöscht werden'));
                setIsLoading(false);
            });
    };

    return (
        <>
            <FormPageWrapper
                title="Link bearbeiten"
                isLoading={isLoading}
                is404={isNotFound}
                hasChanged={hasChanged}
                onSave={handleSave}
                onReset={(editedLink?.id ?? 0) !== 0 ? handleReset : undefined}
                onDelete={(editedLink?.id ?? 0) !== 0 ? () => setConfirmDelete(() => handleDelete) : undefined}
            >
                <TextFieldComponent
                    label="Name des Links"
                    placeholder={'Das hier ist\nein neuer Link'}
                    hint="Der Titel des Links, der auf der Kachel angezeigt wird. Es sind maximal 3 Zeilen möglich."
                    value={editedLink?.text}
                    onChange={(val) => {
                        handlePatch({
                            text: val,
                        });
                    }}
                    required
                    multiline
                    maxCharacters={128}
                />

                <TextFieldComponent
                    label="Link"
                    type="url"
                    placeholder="https://aivot.de/gover"
                    hint="Der Link, welcher aufgerufen wird, sobald eine Mitarbeiter:in auf die Kachel klickt."
                    value={editedLink?.link}
                    onChange={(val) => {
                        handlePatch({
                            link: val,
                        });
                    }}
                    required
                    maxCharacters={128}
                />
            </FormPageWrapper>

            <ConfirmDialog
                title="Link löschen"
                onCancel={() => {
                    setConfirmDelete(undefined);
                }}
                onConfirm={confirmDelete}
            >
                Sind Sie sicher, dass Sie diesen Link wirklich löschen wollen?
                Bitte beachten Sie, dass Sie dies nicht rückgängig machen können.
            </ConfirmDialog>
        </>
    );
}

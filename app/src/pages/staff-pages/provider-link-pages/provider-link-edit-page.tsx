import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {FormEvent, useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Box, Button} from "@mui/material";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showSuccessSnackbar} from "../../../slices/snackbar-slice";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {ProviderLink} from "../../../models/entities/provider-link";
import {ProviderLinksService} from "../../../services/provider-links-service";
import {useChangeBlocker} from "../../../hooks/use-change-blocker";
import {ConfirmDialog} from "../../../dialogs/confirm-dialog/confirm-dialog";

export function ProviderLinkEditPage() {
    useAuthGuard();
    useUserGuard(user => user != null && user.admin);

    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    const {id} = useParams();

    const [originalLink, setOriginalLink] = useState<ProviderLink>();
    const [editedLink, setEditedLink] = useState<ProviderLink>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const hasChanged = useChangeBlocker(originalLink, editedLink);

    useEffect(() => {
        if (id == null || id === 'new') {
            const newDest: ProviderLink = {
                id: 0,
                text: '',
                link: '',
                created: '',
                updated: '',
            };
            setOriginalLink(newDest);
            setEditedLink(newDest);
        } else {
            ProviderLinksService
                .retrieve(parseInt(id))
                .then(link => {
                    setOriginalLink(link);
                    setEditedLink(link);
                });
        }
    }, [id]);

    const patch = (patch: Partial<ProviderLink>) => {
        if (editedLink != null) {
            setEditedLink({
                ...editedLink,
                ...patch,
            });
        }
    };

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (editedLink != null) {
            if (editedLink.id === 0) {
                ProviderLinksService
                    .create(editedLink)
                    .then(createdUser => {
                        setOriginalLink(createdUser);
                        setEditedLink(createdUser);
                        dispatch(showSuccessSnackbar('Link erfolgreich erstellt!'));
                    });
            } else {
                ProviderLinksService
                    .update(editedLink.id, editedLink)
                    .then(updatedUser => {
                        setOriginalLink(updatedUser);
                        setEditedLink(updatedUser);
                        dispatch(showSuccessSnackbar('Link erfolgreich gespeichert!'));
                    });
            }
        }

        return false;
    };

    const handleReset = () => {
        setEditedLink(JSON.parse(JSON.stringify(originalLink!)));
    };

    return (
        <PageWrapper
            title="Link bearbeiten"
            isLoading={editedLink == null}
        >
            {
                editedLink != null &&
                <form onSubmit={handleSubmit}>
                    <TextFieldComponent
                        label="Name des Links"
                        placeholder={'Das hier ist\nein neuer Link'}
                        hint="Der Titel des Links, der auf der Kachel angezeigt wird. Es sind maximal 3 Zeilen möglich."
                        value={editedLink.text}
                        onChange={val => patch({
                            text: val,
                        })}
                        required
                        multiline
                        maxCharacters={128}
                    />

                    <TextFieldComponent
                        label="Link"
                        type="url"
                        placeholder="https://aivot.de/gover"
                        hint="Der Link, welcher aufgerufen wird, sobald eine Nutzer:in auf die Kachel klickt."
                        value={editedLink.link}
                        onChange={val => patch({
                            link: val,
                        })}
                        required
                        maxCharacters={128}
                    />

                    <Box sx={{mt: 4, display: 'flex'}}>
                        <Button
                            type="submit"
                            disabled={!hasChanged}
                        >
                            Speichern
                        </Button>

                        {
                            editedLink.id !== 0 &&
                            <Button
                                sx={{ml: 2}}
                                type="reset"
                                color="error"
                                disabled={!hasChanged}
                                onClick={handleReset}
                            >
                                Zurücksetzen
                            </Button>
                        }

                        {
                            editedLink.id !== 0 &&
                            <Button
                                sx={{ml: 'auto'}}
                                type="button"
                                color="error"
                                onClick={() => setConfirmDelete(() => () => {
                                    ProviderLinksService.destroy(editedLink?.id!);
                                    navigate('/provider-links');
                                })}
                            >
                                Löschen
                            </Button>
                        }
                    </Box>
                </form>
            }

            <ConfirmDialog
                title="Link wirklich löschen"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Sind Sie sicher, dass Sie den Link <strong>{editedLink?.text}</strong> wirklich löschen wollen?
                Bitte beachten Sie, dass dies <u>nicht rückgängig</u> gemacht werden kann!
            </ConfirmDialog>
        </PageWrapper>
    );
}
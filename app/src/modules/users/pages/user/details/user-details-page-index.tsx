import {Box, Button, Link, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {AppConfig} from '../../../../../app-config';
import {StatusTablePropsItem} from '../../../../../components/status-table/status-table-props';
import {StatusTable} from '../../../../../components/status-table/status-table';
import {ApiOutlined, BadgeOutlined, Inventory2Outlined, LockOutlined, MailOutlined, ToggleOnOutlined, WarningOutlined} from '@mui/icons-material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {useNavigate} from 'react-router-dom';
import {isAdmin} from '../../../../../utils/is-admin';
import {useApi} from '../../../../../hooks/use-api';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../../components/generic-details-page/generic-details-page-context';
import {type User} from '../../../../../models/entities/user';
import {UsersApiService} from '../../../users-api-service';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {isApiError} from '../../../../../models/api-error';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import {SubmissionsApiService} from '../../../../submissions/submissions-api-service';
import {ConstraintLinkProps} from '../../../../../dialogs/constraint-dialog/constraint-link-props';
import {ConfirmDialog} from '../../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../../dialogs/constraint-dialog/constraint-dialog';
import {resolveUserName} from '../../../utils/resolve-user-name';

export function UserDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();
    const {
        item: user,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    const [isBusy, setIsBusy] = useState(false);

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedSubmissions, setRelatedSubmissions] = useState<ConstraintLinkProps[] | undefined>(undefined);

    const userInfo: StatusTablePropsItem[] | undefined = useMemo(() => {
        if (user == null) {
            return undefined;
        }

        const userInfoItems: StatusTablePropsItem[] = [
            {
                label: 'Name',
                icon: <BadgeOutlined />,
                children: resolveUserName(user),
            },
            {
                label: 'E-Mail-Adresse',
                icon: <MailOutlined />,
                children: user.email ?
                    <Link
                        href={`mailto:${user.email}`}
                        title="E-Mail an Mitarbeiter:in verfassen (im Standard-Mailprogramm, wenn verfügbar)"
                        sx={{
                            textDecoration: 'none',
                            color: 'inherit',
                        }}
                    >
                        {String(user.email)}
                    </Link> :
                    'Keine E-Mail-Adresse hinterlegt',
            },
            {
                label: 'Passwort',
                icon: <LockOutlined />,
                children: '****************',
            },
            {
                label: 'Kontostatus',
                icon: user?.enabled ? <ToggleOnOutlined /> : <WarningOutlined sx={{color: 'warning.main'}} />,
                children: user?.enabled ?
                    'Aktiv' :
                    <Box
                        sx={{
                            color: 'warning.dark',
                        }}
                    >
                        {user?.deletedInIdp ? 'Gelöscht' : 'Inaktiv'} – {user?.deletedInIdp ?
                            'Dieses Konto wurde im IDP gelöscht und hat keinen Zugriff mehr auf das System.'
                            :
                            'Dieses Konto wurde im IDP deaktiviert und hat derzeit keinen Zugriff auf das System.'
                        }
                    </Box>,
            },
            {
                label: 'Verwendeter IDP',
                icon: <ApiOutlined />,
                children: <Typography>
                    Gover Identity Provider{' '}
                    <Typography component="span" color="text.secondary" fontSize="0.875rem">
                        (basierend auf Keycloak)
                    </Typography>
                </Typography>,
            },
        ];

        if (isAdmin(user)) {
            userInfoItems.splice(4, 0, {
                label: 'Administrator:innen-Status',
                icon: <BadgeOutlined />,
                children: 'Globale Administrator:in – Diese Mitarbeiter:in hat Administratorrechte für das gesamte System.',
            });
        }

        if (user?.deletedInIdp) {
            userInfoItems.push({
                label: 'Hinweis zur Datenhaltung gelöschter Konten',
                icon: <Inventory2Outlined />,
                alignTop: true,
                children:
                    <>
                        <Typography>
                            Diese Mitarbeiter:in wurde im IDP gelöscht. Zur Wahrung der Nachvollziehbarkeit von Zuweisungen, Rollen und Mitgliedschaften (z. B. in Fachbereichen und Anträgen) wird eine pseudonymisierte Version des Nutzerkontos in der Plattform archiviert.
                        </Typography>
                        <Typography sx={{ mt: 2 }}>
                            Diese Maßnahme erfolgt unter Berücksichtigung der DSGVO, insbesondere im Rahmen von Art. 5 Abs. 1 lit. e (Speicherbegrenzung) und Art. 6 Abs. 1 lit. f (berechtigtes Interesse), um die Integrität historischer Systemdaten sicherzustellen, ohne eine Rückverfolgbarkeit der betroffenen Person zu ermöglichen.
                        </Typography>
                    </>,
            });
        }

        return userInfoItems;
    }, [user]);

    if (user == null || userInfo == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const checkAndHandleDelete = async () => {
        if (!user.id) return;

        setIsBusy(true);
        try {
            const submissionsApi = new SubmissionsApiService(api);
            const submissions = await submissionsApi.list(0, 999, undefined, undefined, { assigneeId: user.id, notArchived: true });

            if (submissions.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = submissions.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.fileNumber ? `Antrag mit Aktenzeichen ${f.fileNumber}` : `Antrag ohne Aktenzeichen (${f.id})`,
                    to: `/submissions/${f.id}`
                }));

                if (submissions.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: "Weitere Anträge anzeigen…",
                        to: `/submissions`
                    });
                }

                setRelatedSubmissions(processedLinks);
                setShowConstraintDialog(true);
            } else {
                setConfirmDeleteAction(() => confirmDelete);
            }
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar('Fehler beim Prüfen der Löschbarkeit.'));
        } finally {
            setIsBusy(false);
        }
    };

    const confirmDelete = () => {
        if (!user.id) return;

        setIsBusy(true);
        new UsersApiService(api)
            .destroy(user.id)
            .then(() => {
                navigate('/users', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich gelöscht.'));
            })
            .catch(err => {
                console.error(err);

                if (isApiError(err) && 'message' in err.details) {
                    dispatch(showErrorSnackbar(err.details.message));
                } else {
                    dispatch(showErrorSnackbar('Beim Löschen der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                }
            })
            .finally(() => setIsBusy(false));
    };

    return (
        <>
            <Box sx={{pt: 1.5}}>
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Kontoinformationen dieser Mitarbeiter:in
                </Typography>

                <Typography sx={{mb: 2, maxWidth: 900}}>
                    Die hier angezeigten Kontoinformationen stammen aus dem Identity-Provider-System (IDP) und werden in einer lokal synchronisierten Kopie vorgehalten.
                    Bitte beachten Sie, dass Änderungen an diesen Daten ausschließlich über die Verwaltungsoberfläche des IDP vorgenommen werden können.
                    Änderungen werden im System nach der nächsten Synchronisierung (alle fünf Minuten) sichtbar.
                </Typography>

                <StatusTable
                    cardSx={{
                        mt: 3,
                    }}
                    cardVariant="outlined"
                    items={userInfo}
                />

                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 5,
                        gap: 2,
                    }}
                >
                    {
                        !user.deletedInIdp &&
                            <Button
                            target="_blank"
                            href={`${AppConfig.staff.host}/admin/${AppConfig.staff.realm}/console/#/${AppConfig.staff.realm}/users/${user?.id}/settings`}
                            variant="contained"
                            color="primary"
                            startIcon={<OpenInNewIcon />}
                            disabled={isBusy}
                        >
                            Daten der Mitarbeiter:in verwalten
                        </Button>
                    }

                    {/* TODO: Remove code for user deletion in the future
                        user.deletedInIdp &&
                        <Button
                            onClick={checkAndHandleDelete}
                            variant="outlined"
                            color="error"
                            startIcon={<DeleteOutlinedIcon />}
                            sx={{
                                ml: 'auto',
                            }}
                            disabled={isBusy}
                        >
                            Mitarbeiter:in endgültig löschen
                        </Button>
                    */}
                </Box>
            </Box>

            <ConfirmDialog
                title="Mitarbeiter:in löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Mitarbeiter:in wirklich endgültig löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Mitarbeiter:in kann (noch) nicht gelöscht werden, da sie noch offenen Anträgen zugewiesen ist."
                solutionText="Bitte übertragen Sie die Anträge an eine andere Mitarbeiter:in und versuchen Sie es erneut:"
                links={relatedSubmissions}
            />
        </>
    );
}
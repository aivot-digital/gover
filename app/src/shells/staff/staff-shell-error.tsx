import {Link, useNavigate} from 'react-router-dom';
import {PropsWithChildren, ReactNode} from 'react';
import {Box, Button, Typography} from '@mui/material';
import {ErrorMessage} from '../../slices/shell-slice';
import ScanDelete from '@aivot/mui-material-symbols-400-outlined/dist/scan-delete/ScanDelete';
import {ModuleIcons} from './data/module-icons';
import ArrowBack from '@aivot/mui-material-symbols-400-outlined/dist/arrow-back/ArrowBack';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';
import Warning from '@aivot/mui-material-symbols-400-outlined/dist/warning/Warning';
import Block from '@aivot/mui-material-symbols-400-outlined/dist/block/Block';

interface StaffShellErrorProps {
    error: ErrorMessage;
}

export function StaffShellError(props: StaffShellErrorProps) {
    const {
        error,
    } = props;

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100vh',
                textAlign: 'center',
                padding: 2,
            }}
        >
            <Box
                sx={{
                    width: '24rem',
                }}
            >
                {
                    error != null &&
                    error.status === 403 &&
                    <ErrorDetails
                        icon={<Block />}
                        title="Zugriff verweigert (Fehler 403)"
                        actionBack={false}
                        actionReload={false}
                        actionDashboard={true}
                    >
                        Sie haben keine Berechtigung, auf diese Seite zuzugreifen.
                        Bitte wenden Sie sich an Ihre Administratorin oder Ihren Administrator, wenn Sie glauben, dass es sich um einen Fehler handelt.
                    </ErrorDetails>
                }

                {
                    error != null &&
                    error.status === 404 &&
                    <ErrorDetails
                        icon={<ScanDelete />}
                        title="Seite nicht gefunden (Fehler 404)"
                        actionBack={true}
                        actionReload={false}
                        actionDashboard={true}
                    >
                        Die angeforderte Seite existiert nicht oder wurde möglicherweise entfernt.
                        Bitte überprüfen Sie die URL oder kehren Sie zur vorherigen Seite zurück.
                    </ErrorDetails>
                }

                {
                    error != null &&
                    error.status !== 403 &&
                    error.status !== 404 &&
                    <ErrorDetails
                        icon={<Warning />}
                        title={`Beim Laden ist ein Fehler aufgetreten. (Fehler ${error.status})`}
                        actionBack={false}
                        actionReload={true}
                        actionDashboard={true}
                    >
                        {
                            error.message || 'Bitte prüfen Sie ihre Internetverbindung und versuchen Sie es erneut.'
                        }
                    </ErrorDetails>
                }
            </Box>
        </Box>
    );
}

interface ErrorDetailsProps {
    icon: ReactNode;
    title: string;
    actionBack: boolean;
    actionReload: boolean;
    actionDashboard: boolean;
}

function ErrorDetails(props: PropsWithChildren<ErrorDetailsProps>) {
    const {
        icon,
        title,
        children,
        actionBack,
        actionReload,
        actionDashboard,
    } = props;

    const navigate = useNavigate();

    return (
        <>
            <Box>
                {icon}
            </Box>

            <Typography
                variant="h3"
                component="h1"
            >
                {title}
            </Typography>

            <Typography
                sx={{
                    mt: 2,
                }}
                variant="body1"
            >
                {children}
            </Typography>
            <Box
                sx={{
                    mt: 2,
                    display: 'flex',
                    gap: 2,
                    justifyContent: 'center',
                }}
            >
                {
                    actionBack &&
                    <Button
                        variant="contained"
                        color="inherit"
                        startIcon={<ArrowBack />}
                        onClick={() => navigate(-1)}
                    >
                        Zurück
                    </Button>
                }

                {
                    actionReload &&
                    <Button
                        variant="contained"
                        color="inherit"
                        startIcon={<Refresh />}
                        onClick={() => navigate(0)}
                    >
                        Seite neu laden
                    </Button>
                }

                {
                    actionDashboard &&
                    <Button
                        variant="contained"
                        color="inherit"
                        startIcon={ModuleIcons.dashboard}
                        component={Link}
                        to="/"
                    >
                        Zur Übersicht
                    </Button>
                }
            </Box>
        </>
    );
}
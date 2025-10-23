import Box from '@mui/material/Box';
import {Button, Divider, Paper, Typography} from '@mui/material';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';
import CloudOff from '@aivot/mui-material-symbols-400-outlined/dist/cloud-off/CloudOff';

export function ShellOffline() {
    const handleReload = () => {
        window.location.reload();
    };

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                width: '100%',
                height: '100vh',
                alignItems: 'center',
                justifyContent: 'center',
                textAlign: 'center',
            }}
        >
            <Paper
                sx={{
                    p: 5,
                    maxWidth: '30rem',
                }}
            >
                <CloudOff sx={{ fontSize: 64, mb: 2, color: 'primary' }} />
                <Typography variant="h2" sx={{ mb: 2 }} fontWeight={600}>
                    Verbindung unterbrochen
                </Typography>

                <Typography variant="body1" sx={{ mt: 2 }}>
                    Derzeit besteht keine Verbindung zum Server. Bitte prüfen Sie Ihre Internetverbindung und laden Sie die Seite anschließend neu.
                </Typography>

                <Button
                    onClick={handleReload}
                    variant="contained"
                    startIcon={<Refresh />}
                    color="inherit"
                    sx={{ mt: 3 }}
                >
                    Erneut versuchen
                </Button>

                <Divider sx={{mt: 5, mb: 3}}/>

                <Typography variant="caption" sx={{ mt: 6, color: 'text.secondary' }}>
                    Sollte die Störung weiterhin bestehen, kann eine Beeinträchtigung auf Serverseite vorliegen. Versuchen Sie es zu einem späteren Zeitpunkt erneut, prüfen Sie ggf. die Status-Seite oder kontaktieren Sie den Support (mit Angabe von Zeitpunkt und ausgeführter Aktion).
                </Typography>
            </Paper>
        </Box>
    );
}
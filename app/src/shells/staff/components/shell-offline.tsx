import Box from '@mui/material/Box';
import {Button, Paper, Typography} from '@mui/material';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';

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
            }}
        >
            <Paper
                sx={{
                    p: 4,
                    maxWidth: '24rem',
                }}
            >
                <Typography
                    variant="h5"
                    component="h1"
                    textAlign="center"
                >
                    Keine Verbindung zum Server
                </Typography>

                <Typography
                    variant="body1"
                    sx={{
                        mt: 2,
                    }}
                    textAlign="center"
                >
                    Es konnte keine Verbindung zum Server hergestellt werden.
                    Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es erneut.
                </Typography>

                <Button
                    fullWidth={true}
                    onClick={handleReload}
                    variant="contained"
                    sx={{
                        mt: 4,
                    }}
                    startIcon={
                        <Refresh />
                    }
                >
                    Seite neu laden
                </Button>
            </Paper>
        </Box>
    );
}
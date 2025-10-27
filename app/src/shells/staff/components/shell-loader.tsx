import Box from '@mui/material/Box';
import {LinearProgress, Typography} from '@mui/material';

export function ShellLoader() {
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
            <Typography
                variant="h5"
                component="h1"
            >
                Gover wird geladen…
            </Typography>
            <LinearProgress
                variant="indeterminate"
                sx={{
                    mt: 2,
                    width: '50%',
                    maxWidth: '400px',
                }}
            />
        </Box>
    );
}
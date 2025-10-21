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
                Lade Gover…
            </Typography>
            <LinearProgress
                variant="indeterminate"
                sx={{
                    mt: 1,
                    width: '25%',
                }}
            />
        </Box>
    );
}
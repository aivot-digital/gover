import {Box, Paper, Typography} from '@mui/material';
import {UnderConstructionIllustration} from '../../illustrations/under-construction-illustration';

export function UnderConstructionPage() {
    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100%',
                padding: '2rem',
            }}
        >
            <Box>
                <UnderConstructionIllustration />
                <Typography variant="h5" textAlign="center" marginTop="1rem">
                    Seite im Aufbau. Bitte schauen Sie später noch einmal vorbei.
                </Typography>
            </Box>
        </Box>
    );
}
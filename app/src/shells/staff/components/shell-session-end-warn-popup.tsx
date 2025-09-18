import {useEffect, useState} from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import {getLocalStorageJwt} from '../../../services/base-api-service';
import FrameReload from '@aivot/mui-material-symbols-400-outlined/dist/frame-reload/FrameReload';

const expirationThresholdSeconds = 2 * 60; // 2 minutes

export function ShellSessionEndWarnPopup() {
    const [secondsUntilExpiration, setSecondsUntilExpiration] = useState<number>(0);

    useEffect(() => {
        const interval = setInterval(() => {
            const jwt = getLocalStorageJwt();

            if (jwt == null) {
                setSecondsUntilExpiration(0);
                return;
            }

            const expiresAt = jwt.access.expires;
            const now = Math.floor(Date.now() / 1000);
            const secondsLeft = expiresAt - now;
            setSecondsUntilExpiration(secondsLeft);
        });

        return () => clearInterval(interval);
    }, []);

    if (secondsUntilExpiration <= 0 || secondsUntilExpiration > expirationThresholdSeconds) {
        return null;
    }

    return (
        <Paper>
            <Typography>
                Ihre Sitzung läuft ab in ${} Minuten.
            </Typography>

            <IconButton>
                <FrameReload />
            </IconButton>
        </Paper>
    );
}
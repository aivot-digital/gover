import {useEffect, useState} from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import FrameReload from '@aivot/mui-material-symbols-400-outlined/dist/frame-reload/FrameReload';
import {AuthService} from '../../../services/auth-service';

const expirationThresholdSeconds = 2 * 60; // 2 minutes

export function ShellSessionEndWarnPopup() {
    const [secondsUntilExpiration, setSecondsUntilExpiration] = useState<number>(0);

    useEffect(() => {
        const auth = new AuthService();

        const interval = setInterval(() => {
            const expirationTimestamp = auth.getExpirationTimestamp();

            if (expirationTimestamp == null) {
                setSecondsUntilExpiration(0);
                return;
            }

            const now = Math.floor(Date.now() / 1000);
            const secondsLeft = expirationTimestamp - now;
            setSecondsUntilExpiration(secondsLeft);
        });

        return () => clearInterval(interval);
    }, []);

    const handleReloadAuth = () => {

    };

    if (secondsUntilExpiration <= 0 || secondsUntilExpiration > expirationThresholdSeconds) {
        return null;
    }

    return (
        <Paper>
            <Typography>
                Ihre Sitzung läuft ab in {secondsUntilExpiration} Sekunden ab.
            </Typography>

            <IconButton
                onClick={() => {

                }}
            >
                <FrameReload />
            </IconButton>
        </Paper>
    );
}
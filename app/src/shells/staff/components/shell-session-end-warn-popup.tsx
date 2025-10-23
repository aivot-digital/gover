import {useEffect, useState} from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import {AuthService} from '../../../services/auth-service';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';

const expirationThresholdSeconds = 2 * 60; // 2 minutes

function secondsToMinutesAndSeconds(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`;
}

export function ShellSessionEndWarnPopup() {
    const [secondsUntilExpiration, setSecondsUntilExpiration] = useState<number>(0);

    useEffect(() => {
        const auth = new AuthService();

        const interval = setInterval(() => {
            const expirationTimestampMS = auth.getExpirationTimestamp();
            if (expirationTimestampMS == null) {
                setSecondsUntilExpiration(0);
                return;
            }

            const secondsLeft = Math.floor((expirationTimestampMS - Date.now()) / 1000);
            setSecondsUntilExpiration(secondsLeft);
        }, 1000);

        return () => clearInterval(interval);
    }, []);

    const handleReloadAuth = () => {
        new AuthService().refresh();
    };

    if (secondsUntilExpiration > expirationThresholdSeconds) {
        return null;
    }

    return (
        <Paper
            sx={{
                position: 'fixed',
                bottom: '1rem',
                right: '1rem',
                paddingX: 2,
                paddingY: 1,
                display: 'flex',
                alignItems: 'center',
            }}
        >
            <Typography
                sx={{
                    ml: 1,
                }}
            >
                Ihre Sitzung läuft ab in <span
                style={{
                    display: 'inline-block',
                    textAlign: 'center',
                    width: '3rem',
                }}
            >{secondsToMinutesAndSeconds(secondsUntilExpiration)}</span> ab.
            </Typography>
            <IconButton
                onClick={handleReloadAuth}
            >
                <Refresh />
            </IconButton>
        </Paper>
    );
}
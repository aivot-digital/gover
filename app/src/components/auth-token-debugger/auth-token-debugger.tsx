import React, {useEffect, useRef, useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import {refreshAuthData, selectAuthData} from '../../slices/auth-slice';
import {Paper, Table, TableBody, TableCell, TableRow} from '@mui/material';
import {StorageKey} from '../../data/storage-key';
import {useLocalStorageEffect} from '../../hooks/use-local-storage-effect';
import {useAppDispatch} from '../../hooks/use-app-dispatch';

export function ExpirationTimer() {
    const dispatch = useAppDispatch();
    const [enabled, setEnabled] = useState<boolean | null>(false);

    const authData = useAppSelector(selectAuthData);
    const [accessTokenTimeLeft, setAccessTokenTimeLeft] = useState(0);
    const [refreshTokenTimeLeft, setRefreshTokenTimeLeft] = useState(0);

    const intervalRef = useRef<NodeJS.Timer>();

    useLocalStorageEffect<boolean>(setEnabled, StorageKey.TokenDebuggerActive);

    useEffect(() => {
        if (intervalRef.current) {
            clearInterval(intervalRef.current);
        }

        if (authData == null || authData.accessToken == null || authData.refreshToken == null) {
            return;
        }

        const expiresAccessToken = authData.accessToken.expires;
        const expiresRefreshToken = authData.refreshToken.expires;

        const now = new Date().getTime();
        const timeLeftAccessToken = expiresAccessToken - now;
        const timeLeftRefreshToken = expiresRefreshToken - now;

        setAccessTokenTimeLeft(timeLeftAccessToken / 1000);
        setRefreshTokenTimeLeft(timeLeftRefreshToken / 1000);

        intervalRef.current = setInterval(() => {
            const now = new Date().getTime();
            const timeLeftAccessToken = expiresAccessToken - now;
            const timeLeftRefreshToken = expiresRefreshToken - now;

            setAccessTokenTimeLeft(timeLeftAccessToken / 1000);
            setRefreshTokenTimeLeft(timeLeftRefreshToken / 1000);
        }, 1000);
    }, [authData]);

    if (!enabled) {
        return null;
    }

    const handleRefresh = () => {
        dispatch(refreshAuthData());
    };

    return (
        <Paper
            sx={{
                position: 'fixed',
                bottom: '1rem',
                right: '1rem',
            }}
        >
            <Table size="small">
                <TableBody>
                    <TableRow>
                        <TableCell>
                            Access Token
                        </TableCell>
                        <TableCell
                            sx={{
                                fontFamily: 'monospace',
                            }}
                        >
                            {
                                accessTokenTimeLeft <= 0 &&
                                <a
                                    onClick={handleRefresh}
                                    style={{
                                        cursor: 'pointer',
                                        color: 'blue',
                                        textDecoration: 'underline',
                                        textUnderlineOffset: '4px',
                                    }}
                                >
                                    Refresh
                                </a>
                            }
                            {
                                accessTokenTimeLeft > 0 &&
                                formatSeconds(accessTokenTimeLeft)
                            }
                        </TableCell>
                    </TableRow>
                    <TableRow>
                        <TableCell>
                            Refresh Token
                        </TableCell>
                        <TableCell sx={{fontFamily: 'monospace'}}>
                            {formatSeconds(refreshTokenTimeLeft)}
                        </TableCell>
                    </TableRow>
                </TableBody>
            </Table>
        </Paper>
    );
}

function formatSeconds(seconds: number): string {
    if (seconds < 0) {
        return 'Abgelaufen';
    }

    const secondsStr = (seconds % 60)
        .toFixed(0)
        .padStart(2, '0');

    const minutesStr = (Math.floor(seconds / 60) % 60)
        .toFixed(0)
        .padStart(2, '0');

    const hoursStr = Math.floor(seconds / 3600)
        .toFixed(0)
        .padStart(2, '0');

    return `${hoursStr}:${minutesStr}:${secondsStr}`;
}
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import {getUrlWithoutQuery} from '../../utils/location-utils';
import React, {useEffect, useRef, useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {selectAuthData} from '../../slices/auth-slice';

export function SessionExpiredDialog() {
    const intervalRef = useRef<any>(undefined);
    const authData = useAppSelector(selectAuthData);
    const [isRefreshTokenExpired, setIsRefreshTokenExpired] = useState(false);

    useEffect(() => {
        setIsRefreshTokenExpired(false);

        if (intervalRef.current != null) {
            clearInterval(intervalRef.current);
        }

        const refreshTokenExpiration = authData?.refreshToken?.expires;
        if (refreshTokenExpiration == null) {
            setIsRefreshTokenExpired(true);
            return;
        }

        const now = new Date().getTime();
        const timeLeftRefreshToken = refreshTokenExpiration - now;

        if (timeLeftRefreshToken < 0) {
            setIsRefreshTokenExpired(true);
        } else {
            setIsRefreshTokenExpired(false);
        }

        intervalRef.current = setInterval(() => {
            const now = new Date().getTime();
            const timeLeftRefreshToken = refreshTokenExpiration - now;

            if (timeLeftRefreshToken < 0) {
                setIsRefreshTokenExpired(true);
            } else {
                setIsRefreshTokenExpired(false);
            }
        }, 1000);

        return () => {
            clearInterval(intervalRef.current);
        };
    }, [authData]);

    return (
        <Dialog
            open={isRefreshTokenExpired}
            maxWidth="xs"
        >
            <DialogTitle>
                Sitzung abgelaufen
            </DialogTitle>
            <DialogContent tabIndex={0}>
                <Typography>
                    Ihre Sitzung ist aufgrund von Inaktivität abgelaufen.
                    Bitte melden Sie sich erneut an.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    startIcon={
                        <LoginOutlinedIcon />
                    }
                    component="a"
                    href={`${AppConfig.oidc.hostname}/realms/${AppConfig.oidc.realm}/protocol/openid-connect/auth?${new URLSearchParams({
                        client_id: AppConfig.oidc.client,
                        redirect_uri: getUrlWithoutQuery(),
                        response_type: 'code',
                        scope: 'openid profile email',
                    }).toString()}`}
                >
                    Erneut Anmelden
                </Button>
                <Button
                    sx={{
                        ml: 'auto !important',
                    }}
                    component="a"
                    href="/staff"
                >
                    Zur Startseite
                </Button>
            </DialogActions>
        </Dialog>
    );
}
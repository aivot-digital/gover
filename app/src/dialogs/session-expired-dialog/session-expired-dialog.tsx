import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import {getUrlWithoutQuery} from '../../utils/location-utils';
import {AppConfig} from '../../app-config';
import React, {useEffect, useRef, useState} from 'react';
import {useAppSelector} from '../../hooks/use-app-selector';
import LoginOutlinedIcon from "@mui/icons-material/LoginOutlined";

export function SessionExpiredDialog() {
    const intervalRef = useRef<NodeJS.Timeout>();
    const [open, setOpen] = useState(false);
    const authData = useAppSelector(state => state.auth);

    useEffect(() => {
        if (intervalRef.current != null) {
            clearTimeout(intervalRef.current);
        }

        const refreshToken = authData?.authData?.refreshToken;
        if (refreshToken != null) {
            const expiresInMilliseconds = refreshToken.expires - new Date().getTime();
            intervalRef.current = setTimeout(() => {
                setOpen(true);
            }, expiresInMilliseconds);
        }

        return () => {
            clearTimeout(intervalRef.current);
        };
    }, [authData]);

    return (
        <Dialog open={open} maxWidth={'xs'}>
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
                        <LoginOutlinedIcon/>
                    }
                    component="a"
                    href={`${AppConfig.staff.host}/realms/${AppConfig.staff.realm}/protocol/openid-connect/auth?${new URLSearchParams({
                        client_id: AppConfig.staff.client,
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
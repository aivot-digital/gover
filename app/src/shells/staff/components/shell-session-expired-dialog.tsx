import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import React, {useEffect, useRef, useState} from 'react';
import LoginOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Login';
import {AuthService} from '../../../services/auth-service';
import {useLocation} from 'react-router-dom';
import {createStaffPath} from '../../../utils/url-path-utils';
import {prepareEditorDraftLogin} from '../../../services/editor-draft-login';

export function ShellSessionExpiredDialog() {
    const authService = AuthService;
    const location = useLocation();

    const [isAuthenticated, setIsAuthenticated] = useState(true);
    const loginPending = useRef(false);
    const [preparingLogin, setPreparingLogin] = useState(false);

    const handleLogin = async () => {
        if (loginPending.current) {
            return;
        }
        loginPending.current = true;
        setPreparingLogin(true);
        try {
            const returnId = await prepareEditorDraftLogin();
            window.location.assign(AuthService.getLoginUrl(location, returnId));
        } finally {
            loginPending.current = false;
            setPreparingLogin(false);
        }
    };

    useEffect(() => {
        const intervalPointer = setInterval(() => {
            setIsAuthenticated(authService.isAuthenticated());
        }, 1000);

        return () => {
            clearInterval(intervalPointer);
        };
    }, []);

    return (
        <Dialog
            open={!isAuthenticated}
            maxWidth="xs"
            sx={{
                zIndex: (theme) => Math.max(theme.zIndex.tooltip + 2, 10001),
            }}
        >
            <DialogTitle>
                <Typography
                    variant="h4"
                    component="div"
                >
                    Sitzung abgelaufen
                </Typography>
            </DialogTitle>
            <DialogContent tabIndex={0}>
                <Typography>
                    Ihre Sitzung ist nicht mehr gültig.
                    Bitte melden Sie sich erneut an.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button
                    variant="contained"
                    startIcon={
                        <LoginOutlinedIcon/>
                    }
                    onClick={handleLogin}
                    loading={preparingLogin}
                >
                    Erneut anmelden
                </Button>
                <Button
                    sx={{
                        ml: 'auto !important',
                    }}
                    component="a"
                    href={createStaffPath('/')}
                >
                    Zur Startseite
                </Button>
            </DialogActions>
        </Dialog>
    );
}

import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {AuthService} from '../../../services/auth-service';
import {useLocation} from 'react-router-dom';
import {createStaffPath} from '../../../utils/url-path-utils';

export function ShellSessionExpiredDialog() {
    const authService = AuthService;
    const location = useLocation();

    const [isAuthenticated, setIsAuthenticated] = useState(true);

    const loginUrl = useMemo(() => {
        return AuthService.getLoginUrl(location);
    }, [location]);

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
                    href={loginUrl}
                    disabled={loginUrl.length === 0}
                >
                    Erneut Anmelden
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

import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {AuthService} from '../../../services/auth-service';
import {Link, useLocation} from 'react-router-dom';

export function ShellSessionExpiredDialog() {
    const authService = new AuthService();
    const location = useLocation();

    const [isAuthenticated, setIsAuthenticated] = useState(true);
    const [loginUrl, setLoginUrl] = useState<string>('');

    useEffect(() => {
        authService
            .getLoginUrl()
            .then(setLoginUrl);
    }, [location.pathname, location.search, location.hash]);

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
                    href={loginUrl}
                    disabled={loginUrl.length === 0}
                >
                    Erneut Anmelden
                </Button>
                <Button
                    sx={{
                        ml: 'auto !important',
                    }}
                    component={Link}
                    to="/"
                >
                    Zur Startseite
                </Button>
            </DialogActions>
        </Dialog>
    );
}

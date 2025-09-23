import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography} from '@mui/material';
import React, {useEffect, useState} from 'react';
import LoginOutlinedIcon from '@mui/icons-material/LoginOutlined';
import {AuthService} from '../../../services/auth-service';

export function ShellSessionExpiredDialog() {
    const authService = new AuthService();
    const [isAuthenticated, setIsAuthenticated] = useState(true);

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
                    href={authService.getLoginUrl()}
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
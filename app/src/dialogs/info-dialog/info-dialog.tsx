import {type DialogProps} from '@mui/material/Dialog/Dialog';
import {Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography, useTheme} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';
import React from "react";

interface InfoDialogProps {
    title: string;
    severity: 'success' | 'error' | 'warning' | 'info';
    onClose?: () => void;
}

const severityIconMap: Record<string, any> = {
    'success': CheckCircleOutlinedIcon,
    'error': ErrorOutlineOutlinedIcon,
    'warning': ReportOutlinedIcon,
    'info': InfoOutlinedIcon,
};

export function InfoDialog(props: InfoDialogProps & DialogProps) {
    const theme = useTheme();
    const {
        title,
        severity,
        onClose,
        ...dialogProps
    } = props;

    const Icon = severityIconMap[severity];
    let color: string = theme.palette.primary.main;
    switch (severity) {
        case 'success':
            color = theme.palette.success.main;
            break;
        case 'error':
            color = theme.palette.error.main;
            break;
        case 'info':
            color = theme.palette.info.main;
            break;
        case 'warning':
            color = theme.palette.warning.main;
            break;
    }

    return (
        <Dialog
            {...dialogProps}
            onClose={onClose}
        >
            {
                onClose != null &&
                <DialogTitleWithClose
                    onClose={onClose}
                >
                    <Box sx={{
                        display: 'flex',
                        alignItems: 'center',
                    }}>
                        <Icon
                            sx={{color}}
                        />

                        <Typography
                            sx={{ml: 2}}
                        >
                            {title}
                        </Typography>
                    </Box>
                </DialogTitleWithClose>
            }

            {
                onClose == null &&
                <DialogTitle>
                    <Icon
                        sx={{color}}
                    />

                    <Typography>
                        {title}
                    </Typography>
                </DialogTitle>
            }

            <DialogContent tabIndex={0}>
                {dialogProps.children}
            </DialogContent>

            <DialogActions>
                <Button
                    onClick={props.onClose}
                    variant="contained"
                >
                    Hinweis schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

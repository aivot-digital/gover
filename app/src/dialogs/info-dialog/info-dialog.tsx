import {DialogProps} from "@mui/material/Dialog/Dialog";
import {Box, Dialog, DialogContent, DialogTitle, Typography, useTheme} from "@mui/material";
import {DialogTitleWithClose} from "../../components/static-components/dialog-title-with-close/dialog-title-with-close";
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

interface InfoDialogProps {
    title: string;
    severity: 'success' | 'error' | 'warning' | 'info';
    onClose?: () => void;
}

const severityIconMap: {
    [key: string]: any;
} = {
    'success': CheckCircleOutlinedIcon,
    'error': ErrorOutlineOutlinedIcon,
    'warning': InfoOutlinedIcon,
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
                            sx={{color: color}}
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
                        sx={{color: color}}
                    />

                    <Typography>
                        {title}
                    </Typography>
                </DialogTitle>
            }

            <DialogContent>
                {dialogProps.children}
            </DialogContent>
        </Dialog>
    );
}

import {DialogProps} from "@mui/material/Dialog/Dialog";
import {Box, Dialog, DialogContent, DialogTitle, Typography, useTheme} from "@mui/material";
import {DialogTitleWithClose} from "../../components/static-components/dialog-title-with-close/dialog-title-with-close";
import {faCheckCircle, faExclamationCircle, faInfoCircle} from "@fortawesome/pro-light-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";

interface InfoDialogProps {
    title: string;
    severity: 'success' | 'error' | 'warning' | 'info';
    onClose?: () => void;
}

const severityIconMap: {
    [key: string]: any;
} = {
    'success': faCheckCircle,
    'error': faExclamationCircle,
    'warning': faInfoCircle,
    'info': faInfoCircle,
};

export function InfoDialog(props: InfoDialogProps & DialogProps) {
    const theme = useTheme();

    const {
        title,
        severity,
        onClose,
        ...dialogProps
    } = props;

    const icon = severityIconMap[severity];
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
                    <Box sx={{display: 'flex', alignItems: 'center'}}>
                        <FontAwesomeIcon
                            icon={icon}
                            color={color}
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
                    <FontAwesomeIcon
                        icon={icon}
                        color={color}
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
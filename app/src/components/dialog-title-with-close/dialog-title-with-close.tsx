import {DialogTitle, IconButton, Tooltip, useTheme} from '@mui/material';
import React from 'react';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';

interface DialogTitleProps {
    children?: React.ReactNode;
    onClose: () => void;
    closeTooltip?: string;
    bordered?: boolean;
}

export const DialogTitleWithClose = (props: DialogTitleProps) => {
    const theme = useTheme();
    const {
        children,
        onClose,
        closeTooltip,
        bordered,
        ...other
    } = props;

    return (
        <DialogTitle
            sx={{
                m: 0,
                p: 2,
                pl: 2.8,
                borderBottom: props.bordered ? `1px solid ${theme.palette.grey[300]}` : 'none',
            }}
            {...other}
        >
            {children}

            {
                onClose &&
                <Tooltip title={closeTooltip ?? 'Schließen'}>
                    <IconButton
                        aria-label="close"
                        onClick={onClose}
                        sx={{
                            position: 'absolute',
                            right: 10,
                            top: 8,
                            color: (theme) => theme.palette.grey[500],
                        }}
                    >
                        <CloseOutlinedIcon
                            sx={{width: '24px'}}
                        />
                    </IconButton>
                </Tooltip>
            }
        </DialogTitle>
    );
};

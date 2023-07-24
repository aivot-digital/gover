import {DialogTitle, IconButton, Tooltip} from '@mui/material';
import React from 'react';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';

interface DialogTitleProps {
    children?: React.ReactNode;
    onClose: () => void;
    closeTooltip?: string;
}

export const DialogTitleWithClose = (props: DialogTitleProps) => {
    const {
        children,
        onClose,
        closeTooltip,
        ...other
    } = props;

    return (
        <DialogTitle
            sx={{
                m: 0,
                p: 2,
                pl: 2.8,
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

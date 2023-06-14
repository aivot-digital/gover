import {DialogTitle, IconButton, Tooltip} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faXmark} from '@fortawesome/pro-regular-svg-icons';
import React from 'react';

interface DialogTitleProps {
    id: string;
    children?: React.ReactNode;
    onClose: () => void;
    closeTooltip: string;
}

export const DialogTitleWithClose = (props: DialogTitleProps) => {
    const {children, onClose, closeTooltip, ...other} = props;

    return (
        <DialogTitle
            sx={{
                m: 0,
                p: 2,
                pl: 2.8
            }}
            {...other}
        >
            {children}

            {
                onClose &&
                <Tooltip title={closeTooltip}>
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
                        <FontAwesomeIcon
                            icon={faXmark}
                            style={{width: '24px'}}
                        />
                    </IconButton>
                </Tooltip>
            }
        </DialogTitle>
    );
};

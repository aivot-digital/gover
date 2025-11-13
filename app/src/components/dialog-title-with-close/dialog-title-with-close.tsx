import {Box, DialogTitle, DialogTitleProps, IconButton, Tooltip, Typography, useTheme} from '@mui/material';
import React from 'react';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import {Action} from '../actions/actions-props';
import {Actions} from '../actions/actions';

interface DialogTitleWithCloseProps extends DialogTitleProps {
    children?: React.ReactNode;
    onClose: () => void;
    closeTooltip?: string;
    bordered?: boolean;
    actions?: Action[];
}

export const DialogTitleWithClose = (props: DialogTitleWithCloseProps) => {
    const theme = useTheme();
    const {
        children,
        onClose,
        closeTooltip,
        bordered,
        actions,
        ...other
    } = props;

    return (
        <DialogTitle
            sx={{
                m: 0,
                p: 2,
                pl: 2.8,
                pr: onClose != null ? 8 : 2,
                borderBottom: props.bordered ? `1px solid ${theme.palette.grey[300]}` : 'none',
                display: 'flex',
                justifyContent: 'space-between',
            }}
            {...other}
        >
            <Typography
                variant="h4"
                component="div"
            >
                {children}
            </Typography>

            {
                actions &&
                <Actions
                    actions={actions}
                    sx={{
                        mt: -0.5,
                        justifyContent: 'end',
                    }}
                    dense
                />
            }

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

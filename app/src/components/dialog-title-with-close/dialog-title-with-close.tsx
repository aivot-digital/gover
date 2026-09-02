import {DialogTitle, DialogTitleProps, IconButton, Tooltip, Typography} from '@mui/material';
import React from 'react';
import CloseOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Close';
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
                position: 'relative',
                m: 0,
                p: 2,
                pl: 2.8,
                pr: onClose != null ? 8 : 2,
                borderBottom: props.bordered ? '1px solid' : 'none',
                borderBottomColor: props.bordered ? 'divider' : undefined,
                display: 'flex',
                alignItems: 'center',
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
                        justifyContent: 'end',
                    }}
                    dense
                />
            }

            {
                onClose &&
                <Tooltip title={closeTooltip ?? 'Schließen'}>
                    <IconButton
                        aria-label="Schließen"
                        onClick={onClose}
                        sx={{
                            position: 'absolute',
                            right: 10,
                            top: '50%',
                            transform: 'translateY(-50%)',
                            color: 'text.secondary',
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

import {GenericPageHeaderProps} from './generic-page-header-props';
import {Box, Typography} from '@mui/material';
import React, {useMemo, useReducer} from 'react';
import {Actions} from '../actions/actions';
import {Badge} from '../badge/badge';
import {InfoDialog} from '../../dialogs/info-dialog/info-dialog';
import {Action} from '../actions/actions-props';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';

export function GenericPageHeader(props: GenericPageHeaderProps) {
    const [isHelpDialogOpen, toggleIsHelpDialogOpen] = useReducer((isHelpDialogOpen) => !isHelpDialogOpen, false);

    const actions: Action[] = useMemo(() => {
        const _actions = [
            ...(props.actions ?? []),
        ];

        if (props.actions != null && props.helpDialog != null) {
            _actions.push('separator');
        }

        if (props.helpDialog) {
            _actions.push({
                icon: <HelpOutlineOutlinedIcon />,
                tooltip: props.helpDialog.tooltip,
                onClick: toggleIsHelpDialogOpen,
                ignoreBusy: true,
            });
        }

        return _actions;
    }, [props.actions, props.helpDialog]);

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                {props.icon}

                <Typography
                    variant="h4"
                    component="h1"
                    sx={{
                        marginLeft: 1.5,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        maxWidth: '70%',
                    }}
                    title={props.title}
                >
                    {props.title}
                </Typography>

                {
                    props.badge &&
                    <Badge {...props.badge} />
                }

                <Actions
                    actions={actions}
                    isBusy={props.isBusy}
                    sx={{
                        marginLeft: 'auto',
                    }}
                />
            </Box>

            {
                props.helpDialog &&
                <InfoDialog
                    title={props.helpDialog.title}
                    severity="info"
                    open={isHelpDialogOpen}
                    onClose={toggleIsHelpDialogOpen}
                >
                    {props.helpDialog.content}
                </InfoDialog>
            }
        </>
    );
}
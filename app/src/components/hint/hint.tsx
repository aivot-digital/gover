import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import {HintTooltip} from '../hint-tooltip/hint-tooltip';
import React, {useState} from 'react';
import {HintProps} from './hint-props';
import {Box, Dialog, DialogContent, IconButton, Link, Typography, Button} from '@mui/material';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

export function Hint(props: HintProps) {
    const [open, setOpen] = useState(false);

    return (
        <>
            <HintTooltip
                arrow
                placement="right"
                title={
                    <>
                        <Box>
                            {props.summary}
                        </Box>
                        <Box
                            marginTop={1}
                        >
                            <Link
                                component="button"
                                onClick={() => setOpen(true)}
                            >
                                Mehr...
                            </Link>
                        </Box>
                    </>
                }
            >
                <div>
                    {
                        props.label == null &&
                        <IconButton
                            size="small"
                            sx={{
                                color: props.isError ? 'error.main' : '#a6a6a6',
                                ...props.sx,
                            }}
                            onClick={() => setOpen(true)}
                        >
                            {props.isError ? <ErrorOutlineIcon /> : <HelpIconOutlined />}
                        </IconButton>
                    }

                    {
                        props.label != null &&
                        <Button
                            startIcon={props.isError ? <ErrorOutlineIcon /> : <HelpIconOutlined />}
                            variant="text"
                            size="small"
                            sx={{
                                color: props.isError ? 'error.main' : '#a6a6a6',
                            }}
                            onClick={() => setOpen(true)}
                        >

                            {props.label}
                        </Button>
                    }
                </div>
            </HintTooltip>

            <Dialog
                open={open}
                onClose={() => setOpen(false)}
            >
                <DialogTitleWithClose onClose={() => setOpen(false)}>
                    {props.detailsTitle}
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    {props.details}
                </DialogContent>
            </Dialog>
        </>
    );
}
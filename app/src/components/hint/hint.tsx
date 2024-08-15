import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import {HintTooltip} from '../hint-tooltip/hint-tooltip';
import React, {useState} from 'react';
import {HintProps} from './hint-props';
import {Box, Button, Dialog, DialogContent, IconButton, Link} from '@mui/material';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

export function Hint(props: HintProps): JSX.Element {
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
                <IconButton
                    size="small"
                    sx={{
                        color: '#a6a6a6',
                        ...props.sx,
                    }}
                    onClick={() => setOpen(true)}
                >
                    <HelpIconOutlined />
                </IconButton>
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
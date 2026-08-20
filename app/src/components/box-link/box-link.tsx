import React from 'react';
import {type BoxLinkProps} from './box-link-props';
import NorthWestOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/NorthWest';
import {Box, Link} from '@mui/material';
import Balancer from 'react-wrap-balancer';

export function BoxLink(props: BoxLinkProps) {

    const hasManualLineBreaks = props.text.includes('\n');

    const lines = props
        .text
        .split('\n')
        .map(convertLine);

    return (
        <Link
            href={props.link}
            target="_blank"
            rel="noreferrer"
            underline="none"
            sx={{
                fontSize: '1.5rem',
                lineHeight: 1.2,
                border: '2px solid',
                borderColor: 'primary.main',
                color: 'text.primary',
                transition: '100ms all ease-in-out',
                fontFamily: '"Public Sans", sans-serif',
                fontWeight: 500,
                minHeight: '10rem',
                padding: '1.25rem 1.25rem 3.25rem 1.25rem',
                position: 'relative',
                display: 'block',
                textDecoration: 'none',
                borderRadius: '4px',

                // Hover styles
                '&:hover': {
                    backgroundColor: 'primary.main',
                    color: 'primary.contrastText',
                    cursor: 'pointer',
                },
            }}
        >
            {hasManualLineBreaks ? lines : <Balancer>{props.text}</Balancer>}
            <Box
                component="span"
                sx={{
                    display: 'block',
                    position: 'absolute',
                    bottom: '1rem',
                }}
            >
                <NorthWestOutlinedIcon fontSize="inherit" />
            </Box>
        </Link>
    );
}

function convertLine(line: string, index: number) {
    return (
        <React.Fragment key={index}>
            {line}
            <br/>
        </React.Fragment>
    );
}

import React from 'react';
import {type BoxLinkProps} from './box-link-props';
import NorthWestOutlinedIcon from '@mui/icons-material/NorthWestOutlined';
import { Box, Link } from '@mui/material';

export function BoxLink(props: BoxLinkProps) {
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
                border: '2px solid var(--gover-theme-primary)',
                color: '#16191F',
                transition: '100ms all ease-in-out',
                fontFamily: '"Public Sans", sans-serif',
                fontWeight: 500,
                minHeight: '10rem',
                padding: '1.25rem 1.25rem 3.25rem 1.25rem',
                position: 'relative',
                display: 'block',
                textDecoration: 'none',
                borderRadius: '4px',

                // Child span selector
                '& span': {
                    color: 'var(--gover-theme-primary)',
                },

                // Hover styles
                '&:hover': {
                    backgroundColor: 'var(--gover-theme-primary)',
                    color: 'white',
                    cursor: 'pointer',

                    '& span': {
                        color: 'var(--gover-theme-secondary)',
                    },
                },
            }}
        >
            {lines}
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
    if (index === 0) {
        return (
            <React.Fragment key={index}>
                <span>{line}</span>
                <br/>
            </React.Fragment>
        );
    } else {
        return (
            <React.Fragment key={index}>
                {line}
                <br/>
            </React.Fragment>
        );
    }
}

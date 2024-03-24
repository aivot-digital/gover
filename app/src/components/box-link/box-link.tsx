import React from 'react';
import styles from './box-link.module.scss';
import {type BoxLinkProps} from './box-link-props';
import NorthWestOutlinedIcon from '@mui/icons-material/NorthWestOutlined';

export function BoxLink(props: BoxLinkProps): JSX.Element {
    const lines = props
        .text
        .split('\n')
        .map(convertLine);

    return (
        <a
            href={props.link}
            target="_blank"
            rel="noreferrer"
            className={styles.boxLink /* TODO: Replace with SX Styling */}
        >
            {lines}
            <span className={styles.boxLinkIcon}>
                <NorthWestOutlinedIcon/>
            </span>
        </a>
    );
}

function convertLine(line: string, index: number): JSX.Element {
    if (index === 0) {
        return (
            <React.Fragment key={index}>
                <span>{line}</span><br/>
            </React.Fragment>
        );
    } else {
        return (
            <React.Fragment key={index}>
                {line}<br/>
            </React.Fragment>
        );
    }
}

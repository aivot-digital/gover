import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import styles from './box-link.module.scss';
import {faArrowUpLeft} from '@fortawesome/pro-regular-svg-icons';
import {PropsWithChildren} from 'react';
import {BoxLinkProps} from "./box-link-props";
import NorthWestOutlinedIcon from '@mui/icons-material/NorthWestOutlined';

export function BoxLink(props: PropsWithChildren<BoxLinkProps>) {
    return (
        <a
            href={props.link}
            target="_blank"
            rel="noreferrer"
            className={styles.boxLink}
        >
            {props.children}
            <span className={styles.boxLinkIcon}>
                <NorthWestOutlinedIcon/>
            </span>
        </a>
    );
}

import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import styles from './box-link.module.scss';
import {faArrowUpLeft} from '@fortawesome/pro-light-svg-icons';
import {PropsWithChildren} from 'react';
import {BoxLinkProps} from "./box-link-props";

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
                <FontAwesomeIcon icon={faArrowUpLeft}/>
            </span>
        </a>
    );
}

import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import styles from './box-link.component.module.scss';
import {faArrowUpLeft} from '@fortawesome/pro-light-svg-icons';
import {PropsWithChildren} from 'react';

interface BoxLinkComponentViewProps {
    link: string;
}

export function BoxLinkComponentView(props: PropsWithChildren<BoxLinkComponentViewProps>) {
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

import React from 'react';
import {Alert, AlertTitle} from '@mui/material';
import {type AlertComponentProps} from './alert-component-props';
import {type PropsWithChildren} from 'react';

export function AlertComponent(props: PropsWithChildren<AlertComponentProps>): JSX.Element {
    const renderTextWithParagraphs = (text: string) => {
        const paragraphs = text.split('\n').filter(paragraph => paragraph.length > 0);

        return paragraphs.map((paragraph, index) => (
            <p key={index}
               style={{marginTop: 0, marginBottom: index === paragraphs.length - 1 ? 0 : '1em'}}>
                {paragraph}
            </p>
        ));
    };
    return (
        <Alert
            severity={props.color ?? 'info'}
            sx={{
                my: 4,
                ...props.sx,
            }}
        >
            {
                props.title != null &&
                <AlertTitle>
                    {props.title}
                </AlertTitle>
            }

            {
                props.richtext ?
                    (
                        <div
                            dangerouslySetInnerHTML={{__html: props.text ?? ''}}
                            className={"content-without-margin-on-childs"}
                        />
                    ) :
                    (
                        typeof props.text === 'string' ? renderTextWithParagraphs(props.text) : props.text
                    )
            }

            {props.children}
        </Alert>
    );
}

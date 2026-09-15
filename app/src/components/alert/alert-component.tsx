import React, {type PropsWithChildren} from 'react';
import {Alert, AlertTitle, Box} from '@mui/material';
import {type AlertComponentProps} from './alert-component-props';
import {MarkdownContent} from '../markdown-content/markdown-content';
import {alpha, type Theme} from '@mui/material/styles';

function getSxArray(sx: AlertComponentProps['sx']) {
    if (sx == null) {
        return [];
    }

    return Array.isArray(sx) ? sx : [sx];
}

export function AlertComponent(props: PropsWithChildren<AlertComponentProps>) {
    const severity = props.color ?? 'info';
    const colorVariant = props.colorVariant ?? 'default';

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
            severity={severity}
            sx={colorVariant === 'prominent' ? [
                (theme: Theme) => {
                    const severityColor = theme.palette[severity];

                    return {
                        px: 2,
                        py: 1.5,
                        alignItems: 'flex-start',
                        border: `1px solid ${alpha(theme.palette.common.black, 0.08)}`,
                        borderLeft: `3px solid ${severityColor.main}`,
                        borderRadius: '6px',
                        backgroundColor: alpha(theme.palette.background.paper, 0.98),
                        color: theme.palette.text.primary,
                        boxShadow: `0 10px 24px ${alpha(theme.palette.common.black, 0.12)}`,

                        '& .MuiAlert-icon': {
                            color: severityColor.main,
                            opacity: 1,
                            mt: '2px',
                        },

                        '& .MuiAlert-message': {
                            width: '100%',
                        },

                        '& .MuiAlertTitle-root': {
                            color: theme.palette.text.primary,
                            fontWeight: 700,
                        },
                    };
                },
                ...getSxArray(props.sx),
            ] : {
                ...props.sx,
                px: 2,
                py: 1,
            }}
        >
            <Box sx={{maxWidth: '900px'}}>
                {
                    props.title != null &&
                    <AlertTitle>
                        {props.title}
                    </AlertTitle>
                }

                {
                    props.richtext ?
                        (
                            <MarkdownContent
                                markdown={typeof props.text === 'string' ? props.text : ''}
                                className={"content-without-margin-on-childs"}
                                sx={{
                                    typography: 'body2',
                                }}
                            />
                        ) :
                        (
                            typeof props.text === 'string' ? renderTextWithParagraphs(props.text) : props.text
                        )
                }

                {props.children}

            </Box>
        </Alert>
    );
}

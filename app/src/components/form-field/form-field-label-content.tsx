import {type ReactNode} from 'react';
import {Box} from '@mui/material';

interface FormFieldLabelContentProps {
    children: ReactNode;
    required: boolean;
    showOptionalIndicator?: boolean;
}

export function FormFieldLabelContent(props: FormFieldLabelContentProps) {
    return (
        <>
            {props.children}
            {!props.required && props.showOptionalIndicator !== false && (
                <Box
                    component="span"
                    sx={{
                        color: 'inherit',
                        display: 'inline-block',
                        fontWeight: 400,
                        ml: 0.5,
                        opacity: 0.72,
                        whiteSpace: 'nowrap',
                        wordSpacing: '0.125em',
                    }}
                >
                    – optional
                </Box>
            )}
        </>
    );
}

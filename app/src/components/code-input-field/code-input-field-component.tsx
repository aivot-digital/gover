import {Box, SxProps, Typography} from '@mui/material';
import {CodeEditor} from '../code-editor/code-editor';
import {isStringNullOrEmpty} from '../../utils/string-utils';

export interface CodeInputFieldComponentProps {
    label: string;
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    hint?: string | null | undefined;
    error?: string | null | undefined;
    required?: boolean | null | undefined;
    disabled?: boolean | null | undefined;
    readOnly?: boolean | null | undefined;
    wordWrap?: boolean | null | undefined;
    language?: string | null | undefined;
    height?: string | null | undefined;
    sx?: SxProps | null | undefined;
}

export function CodeInputFieldComponent(props: CodeInputFieldComponentProps) {
    const {
        label,
        value,
        onChange,
        onBlur,
        hint,
        error,
        required,
        disabled,
        readOnly,
        wordWrap,
        language,
        height,
        sx,
    } = props;

    const sxArray = Array.isArray(sx) ? sx : [sx];
    const isReadOnly = Boolean(disabled) || Boolean(readOnly);

    return (
        <Box
            sx={[
                {
                    opacity: disabled ? 0.65 : 1,
                },
                ...sxArray,
            ]}
        >
            <Typography
                sx={{
                    marginBottom: 1,
                    fontWeight: 'medium',
                }}
            >
                {label}{required ? ' *' : ''}
            </Typography>

            <CodeEditor
                value={value ?? ''}
                onChange={(nextValue) => {
                    if (isStringNullOrEmpty(nextValue)) {
                        onChange(null);
                        return;
                    }

                    onChange(nextValue);
                }}
                onBlur={onBlur != null ? (nextValue) => {
                    if (isStringNullOrEmpty(nextValue)) {
                        onBlur(null);
                        return;
                    }

                    onBlur(nextValue);
                } : undefined}
                disabled={disabled ?? false}
                readOnly={isReadOnly}
                wordWrap={wordWrap ?? undefined}
                error={error != null}
                language={language ?? undefined}
                height={height ?? undefined}
                actions={[]}
            />

            {
                (error != null || hint != null) &&
                <Typography
                    sx={{
                        marginTop: 1,
                        color: error != null ? 'error.main' : 'text.secondary',
                    }}
                    variant="caption"
                >
                    {error ?? hint}
                </Typography>
            }
        </Box>
    );
}

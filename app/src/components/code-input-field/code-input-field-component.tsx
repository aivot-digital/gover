import {Box, Dialog, DialogContent, SxProps, Typography} from '@mui/material';
import {useState} from 'react';
import OpenInFull from '@aivot/mui-material-symbols-400-outlined/dist/open-in-full/OpenInFull';
import {CodeEditor} from '../code-editor/code-editor';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {Actions} from '../actions/actions';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';

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
    const displayLabel = `${label}${required ? ' *' : ''}`;
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const handleChange = (nextValue: string) => {
        onChange(normalizeCodeInputValue(nextValue));
    };

    const handleBlur = onBlur != null ? (nextValue: string) => {
        onBlur(normalizeCodeInputValue(nextValue));
    } : undefined;

    const renderEditor = (editorHeight: string | null | undefined) => (
        <CodeEditor
            value={value}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
            readOnly={isReadOnly}
            wordWrap={wordWrap ?? undefined}
            error={error != null}
            language={language ?? undefined}
            height={editorHeight ?? undefined}
            actions={[]}
        />
    );

    return (
        <Box
            sx={[
                {
                    opacity: disabled ? 0.65 : 1,
                },
                ...sxArray,
            ]}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    marginBottom: 1,
                }}
            >
                <Typography
                    sx={{
                        fontWeight: 'medium',
                    }}
                >
                    {displayLabel}
                </Typography>

                <Actions
                    dense
                    size="small"
                    sx={{ml: 'auto'}}
                    actions={[
                        {
                            tooltip: 'In großem Editor öffnen',
                            disabledTooltip: 'Der Editor ist deaktiviert.',
                            ariaLabel: 'In großem Editor öffnen',
                            icon: <OpenInFull fontSize="small"/>,
                            disabled: disabled ?? false,
                            onClick: () => {
                                setIsDialogOpen(true);
                            },
                        },
                    ]}
                />
            </Box>

            {
                !isDialogOpen &&
                renderEditor(height)
            }

            {
                isDialogOpen &&
                <Dialog
                    open
                    onClose={() => setIsDialogOpen(false)}
                    fullWidth
                    maxWidth="xl"
                >
                    <DialogTitleWithClose onClose={() => setIsDialogOpen(false)}>
                        {displayLabel}
                    </DialogTitleWithClose>

                    <DialogContent>
                        {renderEditor('calc(100vh - 220px)')}
                    </DialogContent>
                </Dialog>
            }

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

function normalizeCodeInputValue(value: string): string | null {
    if (isStringNullOrEmpty(value)) {
        return null;
    }

    return value;
}

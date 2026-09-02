import {Dialog, DialogContent, IconButton, Stack, type SxProps, type Theme, Tooltip} from '@mui/material';
import {useState} from 'react';
import OpenInFull from '@aivot/mui-material-symbols-400-n25-outlined/OpenInFull';
import {CodeEditor} from '../code-editor/code-editor';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {DialogTitleWithClose} from '../dialog-title-with-close/dialog-title-with-close';
import {FormField, type FormFieldLayoutProps} from '../form-field';
import {useNormalizedReactId} from '../../hooks/use-normalized-react-id';

export interface CodeInputFieldComponentProps extends FormFieldLayoutProps {
    label: string;
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    onBlur?: (value: string | null) => void;
    hint?: string | null | undefined;
    error?: string | null | undefined;
    required?: boolean | null | undefined;
    disabled?: boolean | null | undefined;
    readOnly?: boolean | null | undefined;
    busy?: boolean | null | undefined;
    wordWrap?: boolean | null | undefined;
    language?: string | null | undefined;
    height?: string | null | undefined;
    controlSx?: SxProps<Theme> | null | undefined;
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
        busy,
        wordWrap,
        language,
        height,
        controlSx,
    } = props;

    const generatedId = useNormalizedReactId();
    const controlId = props.id ?? `code-input-${generatedId}`;
    const dialogId = `${controlId}-dialog`;
    const isReadOnly = Boolean(disabled) || Boolean(readOnly) || Boolean(busy);
    const expandActionLabel = isReadOnly ? 'In großer Ansicht öffnen' : 'In großem Editor öffnen';
    const hasError = error != null && error.length > 0;
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const handleChange = (nextValue: string) => {
        onChange(normalizeCodeInputValue(nextValue));
    };

    const handleBlur = onBlur != null ? (nextValue: string) => {
        onBlur(normalizeCodeInputValue(nextValue));
    } : undefined;

    const renderEditor = (
        editorHeight: string | null | undefined,
        id: string,
        ariaLabel: string,
        ariaLabelledBy?: string,
        ariaDescribedBy?: string,
    ) => (
        <CodeEditor
            id={id}
            ariaLabel={ariaLabel}
            ariaLabelledBy={ariaLabelledBy}
            ariaDescribedBy={ariaDescribedBy}
            value={value}
            onChange={handleChange}
            onBlur={handleBlur}
            disabled={disabled ?? false}
            readOnly={isReadOnly}
            busy={busy ?? false}
            required={required ?? false}
            wordWrap={wordWrap ?? undefined}
            error={hasError}
            language={language ?? undefined}
            height={editorHeight ?? undefined}
            actions={[]}
            sx={controlSx ?? undefined}
        />
    );

    return (
        <FormField
            id={controlId}
            label={label}
            hint={hint}
            error={error}
            required={Boolean(required)}
            disabled={Boolean(disabled)}
            readOnly={Boolean(readOnly)}
            busy={Boolean(busy)}
            ariaLabel={props.ariaLabel}
            ariaDescribedBy={props.ariaDescribedBy}
            margin={props.margin}
            showOptionalIndicator={props.showOptionalIndicator}
            sx={props.sx}
            labelAction={(field) => {
                const suppliedLabelAction = typeof props.labelAction === 'function'
                    ? props.labelAction(field)
                    : props.labelAction;

                return (
                    <Stack direction="row" spacing={0.5} sx={{alignItems: 'center'}}>
                        {suppliedLabelAction}
                        <Tooltip title={expandActionLabel} arrow>
                            <span>
                                <IconButton
                                    size="small"
                                    aria-label={`${label}: ${expandActionLabel}`}
                                    aria-haspopup="dialog"
                                    aria-controls={isDialogOpen ? dialogId : undefined}
                                    aria-expanded={isDialogOpen}
                                    disabled={field.busy}
                                    onClick={() => setIsDialogOpen(true)}
                                >
                                    <OpenInFull fontSize="small"/>
                                </IconButton>
                            </span>
                        </Tooltip>
                    </Stack>
                );
            }}
        >
            {(field) => (
                <>
                    {!isDialogOpen && renderEditor(
                        height,
                        field.controlId,
                        label,
                        field.labelId,
                        field.ariaProps['aria-describedby'],
                    )}

                    {isDialogOpen && (
                        <Dialog
                            open
                            onClose={() => setIsDialogOpen(false)}
                            fullWidth
                            maxWidth="xl"
                            slotProps={{paper: {id: dialogId}}}
                        >
                            <DialogTitleWithClose onClose={() => setIsDialogOpen(false)}>
                                {label}
                            </DialogTitleWithClose>

                            <DialogContent>
                                {renderEditor(
                                    'calc(100vh - 220px)',
                                    `${controlId}-dialog-editor`,
                                    `${label} im großen Editor`,
                                    undefined,
                                    field.ariaProps['aria-describedby'],
                                )}
                            </DialogContent>
                        </Dialog>
                    )}
                </>
            )}

        </FormField>
    );
}

function normalizeCodeInputValue(value: string): string | null {
    if (isStringNullOrEmpty(value)) {
        return null;
    }

    return value;
}

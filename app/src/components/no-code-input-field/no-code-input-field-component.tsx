import {Typography} from '@mui/material';
import {NoCodeEditorWrapper} from '../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {
    NoCodeInputFieldElementItem
} from '../../models/elements/form/input/no-code-input-field-element';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {RootElement} from '../../models/elements/root-element';

interface NoCodeInputFieldComponentProps {
    rootElement: RootElement;
    label: string;
    hint?: string | null;
    error?: string | null;
    required?: boolean | null;
    disabled?: boolean;
    value?: NoCodeInputFieldElementItem | null;
    desiredReturnType: NoCodeDataType;
    onChange: (value: NoCodeInputFieldElementItem | undefined) => void;
}

export function NoCodeInputFieldComponent(props: NoCodeInputFieldComponentProps) {
    const {
        rootElement,
        label,
        hint,
        error,
        required,
        disabled,
        value,
        desiredReturnType,
        onChange,
    } = props;

    return (
        <>
            <NoCodeEditorWrapper
                parents={[rootElement]}
                noCode={value?.noCode ?? undefined}
                onChange={(noCode) => {
                    if (noCode == null) {
                        onChange(undefined);
                        return;
                    }

                    onChange({
                        noCode,
                    });
                }}
                editable={!disabled}
                desiredReturnType={desiredReturnType}
                label={`${label}${required ? ' *' : ''}`}
                hint={hint ?? undefined}
                error={error ?? undefined}
            />

            {
                error != null &&
                <Typography
                    sx={{
                        marginTop: 1,
                        color: 'error.main',
                    }}
                    variant="caption"
                >
                    {error}
                </Typography>
            }
        </>
    );
}

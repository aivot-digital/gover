import {BaseViewProps} from './base-view';
import {Box, Typography} from '@mui/material';
import {CodeInputElement} from '../models/elements/form/input/code-input-element';
import {CodeEditor} from '../components/code-editor/code-editor';

export function CodeInputView(props: BaseViewProps<CodeInputElement, string>) {
    const {
        value,
        setValue,
        element,
    } = props;

    const {
        label,
        hint,
        required,
    } = element;

    return (
        <Box>
            <Typography>
                {label}{required ? ' *' : ''}
            </Typography>

            <CodeEditor
                value={value ?? undefined}
                onChange={(val) => {
                    setValue(val ?? null);
                }}
                actions={[]}
            />
            {
                hint != null &&
                <Typography variant="caption"
                            color="textSecondary">
                    {hint}
                </Typography>
            }
        </Box>
    );
}
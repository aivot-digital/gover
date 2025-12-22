import {BaseViewProps} from "./base-view";
import {Box} from "@mui/material";
import {CodeInputElement} from "../models/elements/form/input/code-input-element";
import {CodeEditor} from "../components/code-editor/code-editor";

export function CodeInputView(props: BaseViewProps<CodeInputElement, string>) {
    const {
        value,
        setValue,
    } = props;

    return (
        <Box>
            <CodeEditor
                value={value ?? undefined}
                onChange={(val) => {
                    setValue(val ?? null);
                }}
                actions={[]}
            />
        </Box>
    );
}
import {BaseViewProps} from "./base-view";
import {RichTextInputElement} from "../models/elements/form/input/rich-text-input-element";
import {RichTextInputComponent} from "../components/rich-text-input-component/rich-text-input-component";

export function RichTextView(props: BaseViewProps<RichTextInputElement, string>) {
    const {
        element,
        value,
        setValue,
    } = props;

    return (
        <RichTextInputComponent
            label={element.label ?? ''}
            hint={element.hint}
            required={element.required}
            value={value}
            onChange={setValue}
        />
    );
}
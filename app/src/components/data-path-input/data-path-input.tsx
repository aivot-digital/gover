import {TextFieldComponent} from '../text-field/text-field-component';
import {type TextFieldComponentProps} from '../text-field/text-field-component-props';
import {validateDataKeyPath} from '../../utils/data-key-input';

export interface DataPathInputProps extends Omit<TextFieldComponentProps, 'startIcon' | 'copyable' | 'copyValueTemplate' | 'pattern' | 'validate'> {
    /** Display/copy prefix only; the stored value remains relative to its data source. */
    prefix: string;
    allowWildcards?: boolean;
    /** Additional context-specific rules, evaluated after the shared path grammar. */
    validatePath?: TextFieldComponentProps['validate'];
}

export function DataPathInput({prefix, allowWildcards = false, validatePath, ...props}: DataPathInputProps) {
    return <TextFieldComponent
        {...props}
        startIcon={prefix}
        copyable
        copyValueTemplate={`${prefix}{value}`}
        validate={(value) => validateDataKeyPath(value, props.required, allowWildcards) ?? validatePath?.(value)}
    />;
}

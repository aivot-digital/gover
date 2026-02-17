import {CheckboxFieldElement} from './checkbox-field-element';
import {DateFieldElement} from './date-field-element';
import {MultiCheckboxFieldElement} from './multi-checkbox-field-element';
import {NumberFieldElement} from './number-field-element';
import {RadioFieldElement} from './radio-field-element';
import {SelectFieldElement} from './select-field-element';
import {TableFieldElement} from './table-field-element';
import {TextFieldElement} from './text-field-element';
import {TimeFieldElement} from './time-field-element';
import {ReplicatingContainerLayout} from '../layout/replicating-container-layout';
import {FileUploadElement} from './file-upload-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {CodeInputElement} from "./code-input-element";
import {FunctionInputElement} from "./function-input-element";
import {RichTextInputElement} from "./rich-text-input-element";

export type AnyInputElement = (
    CheckboxFieldElement |
    DateFieldElement |
    MultiCheckboxFieldElement |
    NumberFieldElement |
    RadioFieldElement |
    SelectFieldElement |
    TableFieldElement |
    TextFieldElement |
    TimeFieldElement |
    FileUploadElement |

    CodeInputElement |
    FunctionInputElement |
    RichTextInputElement |

    ReplicatingContainerLayout
    );

export function isAnyInputElement(obj: any): obj is AnyInputElement {
    return obj != null && 'type' in obj && [
        ElementType.Checkbox,
        ElementType.Date,
        ElementType.MultiCheckbox,
        ElementType.Number,
        ElementType.Radio,
        ElementType.Select,
        ElementType.Table,
        ElementType.Text,
        ElementType.Time,
        ElementType.FileUpload,
        ElementType.CodeInput,
        ElementType.FunctionInput,
        ElementType.RichTextInput,
        ElementType.ReplicatingContainer,
    ].includes(obj.type);
}

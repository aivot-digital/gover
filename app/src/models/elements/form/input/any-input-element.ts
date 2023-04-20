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
import {FileUploadElement} from "./file-upload-element";

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

    ReplicatingContainerLayout
    );

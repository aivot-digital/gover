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
import {CodeInputElement} from './code-input-element';
import {FunctionInputElement} from './function-input-element';
import {RichTextInputElement} from './rich-text-input-element';
import {ChipInputFieldElement} from './chip-input-field-element';
import {DateTimeFieldElement} from './date-time-field-element';
import {DateRangeFieldElement} from './date-range-field-element';
import {TimeRangeFieldElement} from './time-range-field-element';
import {DateTimeRangeFieldElement} from './date-time-range-field-element';
import {MapPointFieldElement} from './map-point-field-element';
import {DomainUserSelectFieldElement} from './domain-user-select-field-element';
import {AssignmentContextFieldElement} from './assignment-context-field-element';
import {DataModelSelectFieldElement} from './data-model-select-field-element';
import {DataObjectSelectFieldElement} from './data-object-select-field-element';
import {NoCodeInputFieldElement} from './no-code-input-field-element';
import {UiDefinitionInputFieldElement} from './ui-definition-input-field-element';
import {ProcessDataKeyInputFieldElement} from './process-data-key-input-field-element';
import {IdentityConfigElement} from './identity-config-element';
import {ProcessInstanceAttachmentSetSelectElement} from './process-instance-attachment-set-select-element';
import {ProcessIdentityIdInputElement} from './process-identity-id-input-element';
import {ElementIsInput} from '../../../../data/element-type/element-is-input';
import {HtmlTemplateInputElement} from './html-template-input-element';
import {PaymentConfigElement} from './payment-config-element';

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
    ChipInputFieldElement |
    DateTimeFieldElement |
    DateRangeFieldElement |
    TimeRangeFieldElement |
    DateTimeRangeFieldElement |
    MapPointFieldElement |
    DomainUserSelectFieldElement |
    AssignmentContextFieldElement |
    DataModelSelectFieldElement |
    DataObjectSelectFieldElement |
    ProcessDataKeyInputFieldElement |
    ProcessInstanceAttachmentSetSelectElement |
    ProcessIdentityIdInputElement |
    HtmlTemplateInputElement |
    UiDefinitionInputFieldElement |
    IdentityConfigElement |
    PaymentConfigElement |
    NoCodeInputFieldElement |

    ReplicatingContainerLayout
    );

export function isAnyInputElement(obj: any): obj is AnyInputElement {
    return obj != null && 'type' in obj && (ElementIsInput[obj.type as ElementType] ?? false);
}

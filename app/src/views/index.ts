import {ElementType} from '../data/element-type/element-type';
import {type BaseView} from './base-view';
import {TextFieldView} from './text-field-view';
import {CheckboxFieldView} from './checkbox-field-view';
import {NumberFieldView} from './number-field-view';
import {AlertView} from './alert-view';
import {SelectFieldView} from './select-field-view';
import {ContainerView} from './container-view';
import {DateFieldView} from './date-field-view';
import {RootComponentView} from '../components/root/root.component.view';
import {StepComponentView} from '../components/step/step.component.view';
import {ImageView} from '../components/image/image-view';
import {HeadlineComponentView} from '../components/headline/headline.component.view';
import {MultiCheckboxFieldComponentView} from '../components/multi-checkbox-field/multi-checkbox-field.component.view';
import {ReplicatingContainerView} from '../components/replicating-container/replicating-container.view';
import {RichtextComponentView} from '../components/richtext/richtext.component.view';
import {RadioFieldComponentView} from '../components/radio-field/radio-field.component.view';
import {SpacerComponentView} from '../components/spacer/spacer.component.view';
import {TableFieldComponentView} from '../components/table-field/table-field.component.view';
import {TimeFieldComponentView} from '../components/time-field/time-field.component.view';
import {GeneralInformationComponentView} from '../components/general-information/general-information.component.view';
import {SummaryComponentView} from '../components/summary/summary.component.view';
import {SubmitComponentView} from '../components/submit/submit.component.view';
import {FileUploadView} from '../components/file-upload-field/file-upload.view';
import {CodeInputView} from './code-input-view';
import {FunctionInputView} from './function-input-view';
import {RichTextView} from './rich-text-input-view';
import {ChipInputFieldView} from './chip-input-field-view';
import {DateTimeFieldView} from './date-time-field-view';
import {DateRangeFieldView} from './date-range-field-view';
import {TimeRangeFieldView} from './time-range-field-view';
import {DateTimeRangeFieldView} from './date-time-range-field-view';
import {MapPointFieldView} from './map-point-field-view';
import {DomainUserSelectFieldView} from './domain-user-select-field-view';
import {AssignmentContextFieldView} from './assignment-context-field-view';
import {DataModelSelectFieldView} from './data-model-select-field-view';
import {DataObjectSelectFieldView} from './data-object-select-field-view';
import {NoCodeInputFieldView} from './no-code-input-field-view';
import {UiDefinitionInputFieldView} from './ui-definition-input-field-view';
import {SummaryLayoutView} from './summary-layout-view';

export const views: Record<ElementType, BaseView<any, any> | null> = {
    [ElementType.FormLayout]: RootComponentView,
    [ElementType.Step]: StepComponentView,
    [ElementType.Alert]: AlertView,
    [ElementType.Image]: ImageView,
    [ElementType.GroupLayout]: ContainerView,
    [ElementType.Checkbox]: CheckboxFieldView,
    [ElementType.Date]: DateFieldView,
    [ElementType.Headline]: HeadlineComponentView,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentView,
    [ElementType.Number]: NumberFieldView,
    [ElementType.ReplicatingContainer]: ReplicatingContainerView,
    [ElementType.RichText]: RichtextComponentView,
    [ElementType.Radio]: RadioFieldComponentView,
    [ElementType.Select]: SelectFieldView,
    [ElementType.Spacer]: SpacerComponentView,
    [ElementType.Table]: TableFieldComponentView,
    [ElementType.Text]: TextFieldView,
    [ElementType.Time]: TimeFieldComponentView,
    [ElementType.IntroductionStep]: GeneralInformationComponentView,
    [ElementType.SummaryStep]: SummaryComponentView,
    [ElementType.SubmitStep]: SubmitComponentView,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: FileUploadView,
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: ContainerView, // TODO
    [ElementType.FunctionInput]: FunctionInputView,
    [ElementType.CodeInput]: CodeInputView,
    [ElementType.RichTextInput]: RichTextView,
    [ElementType.UiDefinitionInput]: UiDefinitionInputFieldView,
    [ElementType.IdentityInput]: null,
    [ElementType.TabLayout]: null,
    [ElementType.ChipInput]: ChipInputFieldView,
    [ElementType.DateTime]: DateTimeFieldView,
    [ElementType.DateRange]: DateRangeFieldView,
    [ElementType.TimeRange]: TimeRangeFieldView,
    [ElementType.DateTimeRange]: DateTimeRangeFieldView,
    [ElementType.MapPoint]: MapPointFieldView,
    [ElementType.DomainAndUserSelect]: DomainUserSelectFieldView,
    [ElementType.AssignmentContext]: AssignmentContextFieldView,
    [ElementType.DataModelSelect]: DataModelSelectFieldView,
    [ElementType.DataObjectSelect]: DataObjectSelectFieldView,
    [ElementType.NoCodeInput]: NoCodeInputFieldView,
    [ElementType.SummaryLayout]: SummaryLayoutView,
};

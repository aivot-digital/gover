import {ElementType} from '../data/element-type/element-type';
import {ElementTypesMap} from '../data/element-type/element-types-map';
import {RootComponentView} from './elements/root/root.component.view';
import {StepComponentView} from './step/step.component.view';
import {AlertComponentView} from './alert/alert.component.view';
import {ContainerComponentView} from './container/container.component.view';
import {CheckboxFieldComponentView} from './checkbox-field/checkbox-field.component.view';
import {DateFieldComponentView} from './date-field/date-field.component.view';
import {HeadlineComponentView} from './headline/headline.component.view';
import {NumberFieldComponentView} from './number-field/number-field.component.view';
import {RichtextComponentView} from './richtext/richtext.component.view';
import {RadioFieldComponentView} from './radio-field/radio-field.component.view';
import {SelectFieldComponentView} from './select-field/select-field.component.view';
import {SpacerComponentView} from './spacer/spacer.component.view';
import {TableFieldComponentView} from './table-field/table-field.component.view';
import {TextFieldComponentView} from './text-field/text-field.component.view';
import {TimeFieldComponentView} from './time-field/time-field.component.view';
import {MultiCheckboxFieldComponentView} from './multi-checkbox-field/multi-checkbox-field.component.view';
import {ReplicatingContainerView} from './replicating-container/replicating-container.view';
import {GeneralInformationComponentView} from './general-information/general-information.component.view';
import {SummaryComponentView} from './summary/summary.component.view';
import {SubmitComponentView} from './submit/submit.component.view';
import {ImageView} from './image/image-view';
import {FileUploadView} from "./file-upload-field/file-upload.view";

export const ViewMap: ElementTypesMap<any> = {
    [ElementType.Root]: RootComponentView,
    [ElementType.Step]: StepComponentView,
    [ElementType.Alert]: AlertComponentView,
    [ElementType.Image]: ImageView,
    [ElementType.Container]: ContainerComponentView,
    [ElementType.Checkbox]: CheckboxFieldComponentView,
    [ElementType.Date]: DateFieldComponentView,
    [ElementType.Headline]: HeadlineComponentView,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentView,
    [ElementType.Number]: NumberFieldComponentView,
    [ElementType.ReplicatingContainer]: ReplicatingContainerView,
    [ElementType.Richtext]: RichtextComponentView,
    [ElementType.Radio]: RadioFieldComponentView,
    [ElementType.Select]: SelectFieldComponentView,
    [ElementType.Spacer]: SpacerComponentView,
    [ElementType.Table]: TableFieldComponentView,
    [ElementType.Text]: TextFieldComponentView,
    [ElementType.Time]: TimeFieldComponentView,
    [ElementType.IntroductionStep]: GeneralInformationComponentView,
    [ElementType.SummaryStep]: SummaryComponentView,
    [ElementType.SubmitStep]: SubmitComponentView,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: FileUploadView,
}

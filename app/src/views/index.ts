import {ElementType} from '../data/element-type/element-type';
import {BaseView} from "./base-view";
import {TextFieldView} from "./text-field-view";
import {ElementTypesMap} from "../data/element-type/element-types-map";
import {CheckboxFieldView} from "./checkbox-field-view";
import {NumberFieldView} from "./number-field-view";
import {AlertView} from "./alert-view";
import {SelectFieldView} from "./select-field-view";
import {ContainerView} from "./container-view";
import {DateFieldView} from "./date-field-view";
import {RootComponentView} from "../components/root/root.component.view";
import {StepComponentView} from "../components/step/step.component.view";
import {ImageView} from "../components/image/image-view";
import {HeadlineComponentView} from "../components/headline/headline.component.view";
import {MultiCheckboxFieldComponentView} from "../components/multi-checkbox-field/multi-checkbox-field.component.view";
import {ReplicatingContainerView} from "../components/replicating-container/replicating-container.view";
import {RichtextComponentView} from "../components/richtext/richtext.component.view";
import {RadioFieldComponentView} from "../components/radio-field/radio-field.component.view";
import {SpacerComponentView} from "../components/spacer/spacer.component.view";
import {TableFieldComponentView} from "../components/table-field/table-field.component.view";
import {TimeFieldComponentView} from "../components/time-field/time-field.component.view";
import {GeneralInformationComponentView} from "../components/general-information/general-information.component.view";
import {SummaryComponentView} from "../components/summary/summary.component.view";
import {SubmitComponentView} from "../components/submit/submit.component.view";
import {FileUploadView} from "../components/file-upload-field/file-upload.view";

const views: ElementTypesMap<BaseView<any, any> | null> = {
    [ElementType.Root]: RootComponentView,
    [ElementType.Step]: StepComponentView,
    [ElementType.Alert]: AlertView,
    [ElementType.Image]: ImageView,
    [ElementType.Container]: ContainerView,
    [ElementType.Checkbox]: CheckboxFieldView,
    [ElementType.Date]: DateFieldView,
    [ElementType.Headline]: HeadlineComponentView,
    [ElementType.MultiCheckbox]: MultiCheckboxFieldComponentView,
    [ElementType.Number]: NumberFieldView,
    [ElementType.ReplicatingContainer]: ReplicatingContainerView,
    [ElementType.Richtext]: RichtextComponentView,
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
};

export default views;

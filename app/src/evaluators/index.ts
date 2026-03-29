import {ElementType} from '../data/element-type/element-type';
import {CheckboxEvaluator} from './checkbox-evaluator';
import {BaseEvaluator} from './base-evaluator';
import {DateEvaluator} from './date-evaluator';
import {MultiCheckboxEvaluator} from './multi-checkbox-evaluator';
import {RadioEvaluator} from './radio-evaluator';
import {SelectEvaluator} from './select-evaluator';
import {NumberEvaluator} from './number-evaluator';
import {TimeEvaluator} from './time-evaluator';
import {TextEvaluator} from './text-evaluator';
import {ReplicatingContainerEvaluator} from './replicating-container-evaluator';
import {ChipInputEvaluator} from './chip-input-evaluator';
import {DateTimeEvaluator} from './date-time-evaluator';
import {DateRangeEvaluator} from './date-range-evaluator';
import {TimeRangeEvaluator} from './time-range-evaluator';
import {DateTimeRangeEvaluator} from './date-time-range-evaluator';
import {MapPointEvaluator} from './map-point-evaluator';
import {DomainUserSelectEvaluator} from './domain-user-select-evaluator';
import {AssignmentContextFieldEvaluator} from './assignment-context-field-evaluator';
import {NoCodeInputEvaluator} from './no-code-input-evaluator';
import {UiDefinitionInputEvaluator} from './ui-definition-input-evaluator';

export const evaluators: {
    [key in ElementType]: BaseEvaluator<any> | null;
} = {
    [ElementType.FormLayout]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.GroupLayout]: null,
    [ElementType.Checkbox]: CheckboxEvaluator,
    [ElementType.Date]: DateEvaluator,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: MultiCheckboxEvaluator,
    [ElementType.Number]: NumberEvaluator,
    [ElementType.ReplicatingContainer]: ReplicatingContainerEvaluator,
    [ElementType.RichText]: null,
    [ElementType.Radio]: RadioEvaluator,
    [ElementType.Select]: SelectEvaluator,
    [ElementType.Spacer]: null,
    [ElementType.Table]: null,
    [ElementType.Text]: TextEvaluator,
    [ElementType.Time]: TimeEvaluator,
    [ElementType.IntroductionStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.Image]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: null,
    [ElementType.DialogLayout]: null,
    [ElementType.StepperLayout]: null,
    [ElementType.ConfigLayout]: null,
    [ElementType.FunctionInput]: null,
    [ElementType.CodeInput]: null,
    [ElementType.RichTextInput]: TextEvaluator,
    [ElementType.UiDefinitionInput]: UiDefinitionInputEvaluator,
    [ElementType.IdentityInput]: null,
    [ElementType.TabLayout]: null,
    [ElementType.ChipInput]: ChipInputEvaluator,
    [ElementType.DateTime]: DateTimeEvaluator,
    [ElementType.DateRange]: DateRangeEvaluator,
    [ElementType.TimeRange]: TimeRangeEvaluator,
    [ElementType.DateTimeRange]: DateTimeRangeEvaluator,
    [ElementType.MapPoint]: MapPointEvaluator,
    [ElementType.DomainAndUserSelect]: DomainUserSelectEvaluator,
    [ElementType.AssignmentContext]: AssignmentContextFieldEvaluator,
    [ElementType.DataModelSelect]: SelectEvaluator,
    [ElementType.DataObjectSelect]: SelectEvaluator,
    [ElementType.NoCodeInput]: NoCodeInputEvaluator,
};

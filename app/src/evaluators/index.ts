import {ElementType} from "../data/element-type/element-type";
import {CheckboxEvaluator} from "./checkbox-evaluator";
import {BaseEvaluator} from "./base-evaluator";
import {DateEvaluator} from "./date-evaluator";
import {MultiCheckboxEvaluator} from "./multi-checkbox-evaluator";
import {RadioEvaluator} from "./radio-evaluator";
import {SelectEvaluator} from "./select-evaluator";
import {NumberEvaluator} from "./number-evaluator";
import {TimeEvaluator} from "./time-evaluator";
import {TextEvaluator} from "./text-evaluator";
import {ReplicatingContainerEvaluator} from './replicating-container-evaluator';

const evaluators: {
    [key in ElementType]: BaseEvaluator<any> | null;
} = {
    [ElementType.Root]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: CheckboxEvaluator,
    [ElementType.Date]: DateEvaluator,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: MultiCheckboxEvaluator,
    [ElementType.Number]: NumberEvaluator,
    [ElementType.ReplicatingContainer]: ReplicatingContainerEvaluator,
    [ElementType.Richtext]: null,
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
};

export default evaluators;

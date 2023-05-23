import {ElementType} from "../data/element-type/element-type";
import {CheckboxEvaluator} from "./checkbox-evaluator";
import {BaseEvaluator} from "./base-evaluator";

const evaluators: {
    [key in ElementType]: BaseEvaluator | null;
} = {
    [ElementType.Root]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: CheckboxEvaluator,
    [ElementType.Date]: null,
    [ElementType.Headline]: null,
    [ElementType.MultiCheckbox]: null,
    [ElementType.Number]: null,
    [ElementType.ReplicatingContainer]: null,
    [ElementType.Richtext]: null,
    [ElementType.Radio]: null,
    [ElementType.Select]: null,
    [ElementType.Spacer]: null,
    [ElementType.Table]: null,
    [ElementType.Text]: null,
    [ElementType.Time]: null,
    [ElementType.IntroductionStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.Image]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: null,
};

export default evaluators;

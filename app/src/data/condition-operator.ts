import {ElementType} from "./element-type/element-type";

export enum ConditionOperator {
    Equals = 0,
    NotEquals = 1,
    LessThan = 2,
    LessThanOrEqual = 3,
    GreaterThan = 4,
    GreaterThanOrEqual = 5,
    Includes = 6,
    NotIncludes = 7,
    StartsWith = 8,
    NotStartsWith = 9,
    EndsWith = 10,
    NotEndsWith = 11,
    MatchesPattern = 12,
    NotMatchesPattern = 13,
    IncludesPattern = 14,
    NotIncludesPattern = 15,
}

export const ConditionOperatorLabel: {
    [key in ConditionOperator]: string;
} = {
    [ConditionOperator.Equals]: "gleich",
    [ConditionOperator.NotEquals]: "ungleich",
    [ConditionOperator.LessThan]: "weniger als",
    [ConditionOperator.LessThanOrEqual]: "weniger als oder gleich",
    [ConditionOperator.GreaterThan]: "größer als",
    [ConditionOperator.GreaterThanOrEqual]: "größer als oder gleich",
    [ConditionOperator.Includes]: "beinhaltet",
    [ConditionOperator.NotIncludes]: "beinhaltet nicht",
    [ConditionOperator.StartsWith]: "beginnt mit",
    [ConditionOperator.NotStartsWith]: "beginnt nicht mit",
    [ConditionOperator.EndsWith]: "endet mit",
    [ConditionOperator.NotEndsWith]: "endet nicht mit",
    [ConditionOperator.MatchesPattern]: "entspricht Muster",
    [ConditionOperator.NotMatchesPattern]: "entspricht Muster nicht",
    [ConditionOperator.IncludesPattern]: "beinhaltet Muster",
    [ConditionOperator.NotIncludesPattern]: "beinhaltet Muster nicht",
};

export const ConditionOperatorLimiter: {
    [key in ElementType]: ConditionOperator[];
} = {
    [ElementType.Root]: [],
    [ElementType.Step]: [],
    [ElementType.Alert]: [],
    [ElementType.Container]: [],
    [ElementType.Checkbox]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
    ],
    [ElementType.Date]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
    ],
    [ElementType.Headline]: [],
    [ElementType.MultiCheckbox]: [
        ConditionOperator.Includes,
        ConditionOperator.NotIncludes,
    ],
    [ElementType.Number]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
        ConditionOperator.LessThan,
        ConditionOperator.LessThanOrEqual,
        ConditionOperator.GreaterThan,
        ConditionOperator.GreaterThanOrEqual,
    ],
    [ElementType.ReplicatingContainer]: [],
    [ElementType.Richtext]: [],
    [ElementType.Radio]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
    ],
    [ElementType.Select]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
    ],
    [ElementType.Spacer]: [],
    [ElementType.Table]: [],
    [ElementType.Text]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
        ConditionOperator.Includes,
        ConditionOperator.NotIncludes,
        ConditionOperator.StartsWith,
        ConditionOperator.NotStartsWith,
        ConditionOperator.EndsWith,
        ConditionOperator.NotEndsWith,
        ConditionOperator.MatchesPattern,
        ConditionOperator.NotMatchesPattern,
        ConditionOperator.IncludesPattern,
        ConditionOperator.NotIncludesPattern,
    ],
    [ElementType.Time]: [
        ConditionOperator.Equals,
        ConditionOperator.NotEquals,
    ],
    [ElementType.IntroductionStep]: [],
    [ElementType.SubmitStep]: [],
    [ElementType.SummaryStep]: [],
    [ElementType.Image]: [],
    [ElementType.SubmittedStep]: [],
    [ElementType.FileUpload]: []
};

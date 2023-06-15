import { ElementType } from "./element-type/element-type";
import {ElementTypesMap} from "./element-type/element-types-map";

export enum ConditionOperator {
    Equals = 0,
    EqualsIgnoreCase = 16,
    NotEquals = 1,
    NotEqualsIgnoreCase = 17,
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
    Empty = 18, // TODO: Implement on server
    NotEmpty = 19, // TODO: Implement on server
}

export const ConditionOperatorLabel: {
    [key in ConditionOperator]: string;
} = {
    [ConditionOperator.Equals]: "gleich",
    [ConditionOperator.EqualsIgnoreCase]: "gleich (beachtet keine Groß-/Kleinschreibung)",
    [ConditionOperator.NotEquals]: "ungleich",
    [ConditionOperator.NotEqualsIgnoreCase]: "ungleich (beachtet keine Groß-/Kleinschreibung)",
    [ConditionOperator.LessThan]: "kleiner als",
    [ConditionOperator.LessThanOrEqual]: "kleiner als oder gleich",
    [ConditionOperator.GreaterThan]: "größer als",
    [ConditionOperator.GreaterThanOrEqual]: "größer als oder gleich",
    [ConditionOperator.Includes]: "beinhaltet",
    [ConditionOperator.NotIncludes]: "beinhaltet nicht",
    [ConditionOperator.StartsWith]: "beginnt mit",
    [ConditionOperator.NotStartsWith]: "beginnt nicht mit",
    [ConditionOperator.EndsWith]: "endet mit",
    [ConditionOperator.NotEndsWith]: "endet nicht mit",
    [ConditionOperator.MatchesPattern]: "entspricht Muster",
    [ConditionOperator.NotMatchesPattern]: "entspricht nicht Muster",
    [ConditionOperator.IncludesPattern]: "beinhaltet Muster",
    [ConditionOperator.NotIncludesPattern]: "beinhaltet nicht Muster",
    [ConditionOperator.Empty]: "ist leer",
    [ConditionOperator.NotEmpty]: "ist nicht leer",
};

export const ConditionOperatorIsUnary: {
    [key in ConditionOperator]: boolean;
} = {
    [ConditionOperator.Equals]: false,
    [ConditionOperator.EqualsIgnoreCase]: false,
    [ConditionOperator.NotEquals]: false,
    [ConditionOperator.NotEqualsIgnoreCase]: false,
    [ConditionOperator.LessThan]: false,
    [ConditionOperator.LessThanOrEqual]: false,
    [ConditionOperator.GreaterThan]: false,
    [ConditionOperator.GreaterThanOrEqual]: false,
    [ConditionOperator.Includes]: false,
    [ConditionOperator.NotIncludes]: false,
    [ConditionOperator.StartsWith]: false,
    [ConditionOperator.NotStartsWith]: false,
    [ConditionOperator.EndsWith]: false,
    [ConditionOperator.NotEndsWith]: false,
    [ConditionOperator.MatchesPattern]: false,
    [ConditionOperator.NotMatchesPattern]: false,
    [ConditionOperator.IncludesPattern]: false,
    [ConditionOperator.NotIncludesPattern]: false,
    [ConditionOperator.Empty]: true,
    [ConditionOperator.NotEmpty]: true,
};

export const ConditionOperatorMessage: {
    [key in ConditionOperator]: (valueA: any, valueB: any) => string;
} = {
    [ConditionOperator.Equals]: (valueA, valueB) => `${valueA} muss gleich ${valueB} sein.`,
    [ConditionOperator.EqualsIgnoreCase]: (valueA, valueB) => `${valueA} muss gleich ${valueB} sein.`,
    [ConditionOperator.NotEquals]: (valueA, valueB) => `${valueA} darf nicht gleich ${valueB} sein.`,
    [ConditionOperator.NotEqualsIgnoreCase]: (valueA, valueB) => `${valueA} darf nicht gleich ${valueB} sein.`,
    [ConditionOperator.LessThan]: (valueA, valueB) => `${valueA} muss kleiner als ${valueB} sein.`,
    [ConditionOperator.LessThanOrEqual]: (valueA, valueB) => `${valueA} muss kleiner oder gleich ${valueB} sein.`,
    [ConditionOperator.GreaterThan]: (valueA, valueB) => `${valueA} muss größer als ${valueB} sein.`,
    [ConditionOperator.GreaterThanOrEqual]: (valueA, valueB) => `${valueA} muss größer oder gleich ${valueB} sein.`,
    [ConditionOperator.Includes]: (valueA, valueB) => `${valueA} muss ${valueB} enthalten.`,
    [ConditionOperator.NotIncludes]: (valueA, valueB) => `${valueA} darf ${valueB} nicht enthalten.`,
    [ConditionOperator.StartsWith]: (valueA, valueB) => `${valueA} muss mit ${valueB} beginnen.`,
    [ConditionOperator.NotStartsWith]: (valueA, valueB) => `${valueA} darf nicht mit ${valueB} beginnen.`,
    [ConditionOperator.EndsWith]: (valueA, valueB) => `${valueA} muss mit ${valueB} enden.`,
    [ConditionOperator.NotEndsWith]: (valueA, valueB) => `${valueA} darf nicht mit ${valueB} enden.`,
    [ConditionOperator.MatchesPattern]: (valueA, valueB) => `${valueA} muss dem Muster ${valueB} entsprechen.`,
    [ConditionOperator.NotMatchesPattern]: (valueA, valueB) => `${valueA} darf nicht dem Muster ${valueB} entsprechen.`,
    [ConditionOperator.IncludesPattern]: (valueA, valueB) =>  `${valueA} muss das Muster ${valueB} enthalten.`,
    [ConditionOperator.NotIncludesPattern]: (valueA, valueB) => `${valueA} darf das Muster ${valueB} nicht enthalten.`,
    [ConditionOperator.Empty]: (valueA, valueB) => `${valueA} muss ausgefüllt sein.`,
    [ConditionOperator.NotEmpty]: (valueA, valueB) => `${valueA} darf nicht ausgefüllt sein.`,
};

export const ConditionOperatorHint: ElementTypesMap<string | null> = {
    [ElementType.Root]: null,
    [ElementType.Step]: null,
    [ElementType.Alert]: null,
    [ElementType.Container]: null,
    [ElementType.Checkbox]: null,
    [ElementType.Date]: 'Bitte im Format TT.MM.JJJJ / MM.JJJJ / JJJJ / TT.MM. / TT. eingeben.',
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
    [ElementType.Time]: 'Bitte im Format HH:MM eingeben.',
    [ElementType.IntroductionStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.Image]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: null
};

export const ConditionOperatorAdditionalHint: {
    [key in ConditionOperator]: string | null;
} = {
    [ConditionOperator.Equals]: null,
    [ConditionOperator.EqualsIgnoreCase]: null,
    [ConditionOperator.NotEquals]: null,
    [ConditionOperator.NotEqualsIgnoreCase]: null,
    [ConditionOperator.LessThan]: null,
    [ConditionOperator.LessThanOrEqual]: null,
    [ConditionOperator.GreaterThan]: null,
    [ConditionOperator.GreaterThanOrEqual]: null,
    [ConditionOperator.Includes]: null,
    [ConditionOperator.NotIncludes]: null,
    [ConditionOperator.StartsWith]: null,
    [ConditionOperator.NotStartsWith]: null,
    [ConditionOperator.EndsWith]: null,
    [ConditionOperator.NotEndsWith]: null,
    [ConditionOperator.MatchesPattern]: 'Das Muster muss als regulärer Ausdruck eingegeben werden, welcher sowohl für JavaScript als auch Java gültig ist.',
    [ConditionOperator.NotMatchesPattern]: 'Das Muster muss als regulärer Ausdruck eingegeben werden, welcher sowohl für JavaScript als auch Java gültig ist.',
    [ConditionOperator.IncludesPattern]: 'Das Muster muss als regulärer Ausdruck eingegeben werden, welcher sowohl für JavaScript als auch Java gültig ist.',
    [ConditionOperator.NotIncludesPattern]: 'Das Muster muss als regulärer Ausdruck eingegeben werden, welcher sowohl für JavaScript als auch Java gültig ist.',
    [ConditionOperator.Empty]: null,
    [ConditionOperator.NotEmpty]: null,
};
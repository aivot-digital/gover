import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';
import {
    faArrowCircleRight,
    faBookmarkCircle,
    faCircleBookOpen,
    faCircleHeart,
    faCircleStar, faEnvelopeCircle,
    faExclamationCircle,
    faInfoCircle,
    faPenCircle,
    faQuestionCircle,
    faUserCircle
} from '@fortawesome/pro-light-svg-icons';
import {StepIcon} from '../models/step-icon';
import {ElementType} from './element-type/element-type';
import {StepElement} from "../models/elements/steps/step-element";
import {IntroductionStepElement} from "../models/elements/steps/introduction-step-element";
import {SummaryStepElement} from "../models/elements/steps/summary-step-element";
import {SubmitStepElement} from "../models/elements/steps/submit-step-element";
import {SubmittedStepElement} from "../models/elements/steps/submitted-step-element";

export const StepIcons: StepIcon[] = [
    {
        id: 'arrow',
        def: faArrowCircleRight,
        label: 'Pfeil',
    },
    {
        id: 'info',
        def: faInfoCircle,
        label: 'Info',
    },
    {
        id: 'warning',
        def: faExclamationCircle,
        label: 'Warnung',
    },
    {
        id: 'question',
        def: faQuestionCircle,
        label: 'Frage',
    },
    {
        id: 'star',
        def: faCircleStar,
        label: 'Stern',
    },
    {
        id: 'user',
        def: faUserCircle,
        label: 'Benutzerdaten',
    },
    {
        id: 'pen',
        def: faPenCircle,
        label: 'Stift',
    },
    {
        id: 'bookmark',
        def: faBookmarkCircle,
        label: 'Lesezeichen',
    },
    {
        id: 'openBook',
        def: faCircleBookOpen,
        label: 'Offenes Buch',
    },
    {
        id: 'heart',
        def: faCircleHeart,
        label: 'Herz',
    },
];

export const StepIconsMap = StepIcons.reduce((acc, val) => ({
    ...acc,
    [val.id]: val,
}), {} as any);

export function getStepIcon(step: StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement | SubmittedStepElement): IconDefinition {
    switch (step.type) {
        case ElementType.Step:
            if (step.icon != null && step.icon in StepIconsMap) {
                return StepIconsMap[step.icon].def;
            }
            return faArrowCircleRight;
        case ElementType.IntroductionStep:
            return faInfoCircle;
        case ElementType.SummaryStep:
            return faExclamationCircle;
        case ElementType.SubmitStep:
            return faEnvelopeCircle;
        default:
            return faArrowCircleRight;
    }

}

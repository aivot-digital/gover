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
import {AnyStepElement} from '../models/elements/step-elements/any-step-element';
import {ElementType} from './element-type/element-type';

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

export function getStepIcon(step: AnyStepElement): IconDefinition {
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

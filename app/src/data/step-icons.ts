import {type StepIcon} from '../models/step-icon';
import {ElementType} from './element-type/element-type';
import {type StepElement} from '../models/elements/steps/step-element';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../models/elements/steps/submit-step-element';
import {type SubmittedStepElement} from '../models/elements/steps/submitted-step-element';
import {type SvgIcon} from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import LightbulbCircleOutlinedIcon from '@mui/icons-material/LightbulbCircleOutlined';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import FlagCircleOutlinedIcon from '@mui/icons-material/FlagCircleOutlined';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import CircleNotificationsOutlinedIcon from '@mui/icons-material/CircleNotificationsOutlined';
import CloudCircleOutlinedIcon from '@mui/icons-material/CloudCircleOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import StarsOutlinedIcon from '@mui/icons-material/StarsOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import BookmarkBorderOutlinedIcon from '@mui/icons-material/BookmarkBorderOutlined';
import AutoStoriesOutlinedIcon from '@mui/icons-material/AutoStoriesOutlined';
import FavoriteBorderOutlinedIcon from '@mui/icons-material/FavoriteBorderOutlined';
import ArrowCircleRightOutlinedIcon from '@mui/icons-material/ArrowCircleRightOutlined';
import AccountBalanceOutlinedIcon from '@mui/icons-material/AccountBalanceOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';

export const StepIcons: StepIcon[] = [
    {
        id: 'arrow',
        def: ArrowCircleRightOutlinedIcon,
        label: 'Pfeil',
    },
    {
        id: 'info',
        def: InfoOutlinedIcon,
        label: 'Info',
    },
    {
        id: 'warning',
        def: ErrorOutlineOutlinedIcon,
        label: 'Warnung',
    },
    {
        id: 'question',
        def: HelpOutlineOutlinedIcon,
        label: 'Frage',
    },
    {
        id: 'star',
        def: StarsOutlinedIcon,
        label: 'Stern',
    },
    {
        id: 'user',
        def: AccountCircleOutlinedIcon,
        label: 'Benutzerdaten',
    },
    {
        id: 'cloud',
        def: CloudCircleOutlinedIcon,
        label: 'Wolke',
    },
    {
        id: 'plus',
        def: AddCircleOutlineOutlinedIcon,
        label: 'Plus',
    },
    {
        id: 'flag',
        def: FlagCircleOutlinedIcon,
        label: 'Flagge',
    },
    {
        id: 'bell',
        def: CircleNotificationsOutlinedIcon,
        label: 'Glocke',
    },
    {
        id: 'lightbulb',
        def: LightbulbCircleOutlinedIcon,
        label: 'Glühbirne',
    },
    {
        id: 'build',
        def: BuildCircleOutlinedIcon,
        label: 'Werkzeug',
    },
    {
        id: 'pen',
        def: EditOutlinedIcon,
        label: 'Stift',
    },
    {
        id: 'bookmark',
        def: BookmarkBorderOutlinedIcon,
        label: 'Lesezeichen',
    },
    {
        id: 'openBook',
        def: AutoStoriesOutlinedIcon,
        label: 'Offenes Buch',
    },
    {
        id: 'heart',
        def: FavoriteBorderOutlinedIcon,
        label: 'Herz',
    },
    {
        id: 'bank',
        def: AccountBalanceOutlinedIcon,
        label: 'Bank',
    },
    {
        id: 'envelope',
        def: EmailOutlinedIcon,
        label: 'Brief',
    },
];

export const StepIconsMap = StepIcons.reduce<any>((acc, val) => ({
    ...acc,
    [val.id]: val,
}), {});

export function getStepIcon(step: StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement | SubmittedStepElement): typeof SvgIcon {
    switch (step.type) {
        case ElementType.Step:
            if (step.icon != null && step.icon in StepIconsMap) {
                return StepIconsMap[step.icon].def;
            }
            return ArrowCircleRightOutlinedIcon;
        case ElementType.IntroductionStep:
            return InfoOutlinedIcon;
        case ElementType.SummaryStep:
            return ErrorOutlineOutlinedIcon;
        case ElementType.SubmitStep:
            return CheckCircleOutlineOutlinedIcon;
        default:
            return ArrowCircleRightOutlinedIcon;
    }
}

import {type StepIcon} from '../models/step-icon';
import {ElementType} from './element-type/element-type';
import {type StepElement} from '../models/elements/steps/step-element';
import {type IntroductionStepElement} from '../models/elements/steps/introduction-step-element';
import {type SummaryStepElement} from '../models/elements/steps/summary-step-element';
import {type SubmitStepElement} from '../models/elements/steps/submit-step-element';
import {type SubmittedStepElement} from '../models/elements/steps/submitted-step-element';
import {type SvgIconComponent} from '../types/svg-icon-component';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import LightbulbCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LightbulbCircle';
import AccountCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import FlagCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/FlagCircle';
import BuildCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/BuildCircle';
import AddCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AddCircle';
import CircleNotificationsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CircleNotifications';
import CloudCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CloudCircle';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import HelpOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import StarsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Stars';
import EditOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import BookmarkBorderOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Bookmark';
import AutoStoriesOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AutoStories';
import FavoriteBorderOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Favorite';
import ArrowCircleRightOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ArrowCircleRight';
import AccountBalanceOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccountBalance';
import CheckCircleOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import EmailOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Mail';
import DirectionsCarFilledOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/DirectionsCar';
import LocalShippingOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LocalShipping';
import FamilyRestroomOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/FamilyRestroom';
import ChecklistOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Checklist';
import FormatListBulletedOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/FormatListBulleted';
import LocationOnOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LocationOn';
import HomeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Home';
import FactoryOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Factory';
import EscalatorWarningOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/EscalatorWarning';
import EuroOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Euro';
import ReceiptLongOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ReceiptLong';
import AccessTimeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Schedule';
import CalendarMonthOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CalendarMonth';
import AttachFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AttachFile';
import ChangeCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ChangeCircle';
import SwapHorizontalCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SwapHorizontalCircle';
import BoltOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Bolt';
import GavelOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Gavel';
import BadgeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Badge';
import PaymentOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CreditCard';
import LockOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Lock';
import WorkOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Work';
import SchoolOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/School';
import PetsOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Pets';
import PeopleAltOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Groups';
import StorefrontOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Storefront';
import PublicOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Public';
import MobileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Mobile';
import ConstructionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Construction';
import InsertDriveFileOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Draft';
import QuestionAnswerOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Forum';
import VerifiedUserOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/VerifiedUser';
import HowToRegOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/HowToReg';

export const StepIcons: StepIcon[] = [
    // Allgemein / Navigation
    {id: 'arrow', def: ArrowCircleRightOutlinedIcon, label: 'Weiter (Pfeil)'},
    {id: 'info', def: InfoOutlinedIcon, label: 'Information (i-Symbol)'},
    {id: 'question', def: HelpOutlineOutlinedIcon, label: 'Frage (Fragezeichen)'},
    {id: 'warning', def: ErrorOutlineOutlinedIcon, label: 'Warnung (Ausrufezeichen)'},
    {id: 'check', def: CheckCircleOutlineOutlinedIcon, label: 'Bestätigung (Haken)'},
    {id: 'change', def: ChangeCircleOutlinedIcon, label: 'Änderung (Kreis mit Pfeilen)'},
    {id: 'swap', def: SwapHorizontalCircleOutlinedIcon, label: 'Tausch (Wechselpfeile)'},
    {id: 'bolt', def: BoltOutlinedIcon, label: 'Schnellaktion (Blitz)'},
    {id: 'calendar', def: CalendarMonthOutlinedIcon, label: 'Kalender (Monatssymbol)'},
    {id: 'clock', def: AccessTimeOutlinedIcon, label: 'Zeit (Uhr)'},
    {id: 'location', def: LocationOnOutlinedIcon, label: 'Standort (Kartenpin)'},

    // Personen & Gruppen
    {id: 'user', def: AccountCircleOutlinedIcon, label: 'Person (Benutzersymbol)'},
    {id: 'family', def: FamilyRestroomOutlinedIcon, label: 'Familie (Eltern mit Kind)'},
    {id: 'personWithChild', def: EscalatorWarningOutlinedIcon, label: 'Begleitperson (Kind an der Hand)'},
    {id: 'group', def: PeopleAltOutlinedIcon, label: 'Personengruppe (Mehrere Personen)'},
    {id: 'approval', def: HowToRegOutlinedIcon, label: 'Genehmigung (Person mit Haken)'},
    {id: 'badge', def: BadgeOutlinedIcon, label: 'Identifikation (Ausweissymbol)'},
    {id: 'trust', def: VerifiedUserOutlinedIcon, label: 'Verifizierung (Benutzer mit Schild)'},
    {id: 'lock', def: LockOutlinedIcon, label: 'Sicherheit (Schloss)'},

    // Dokumente & Kommunikation
    {id: 'envelope', def: EmailOutlinedIcon, label: 'E-Mail (Briefumschlag)'},
    {id: 'attachment', def: AttachFileOutlinedIcon, label: 'Anhang (Büroklammer)'},
    {id: 'document', def: InsertDriveFileOutlinedIcon, label: 'Dokument (Dateiblatt)'},
    {id: 'dialogue', def: QuestionAnswerOutlinedIcon, label: 'Rückfrage (Sprechblasen)'},
    {id: 'bookmark', def: BookmarkBorderOutlinedIcon, label: 'Lesezeichen (Marker)'},
    {id: 'openBook', def: AutoStoriesOutlinedIcon, label: 'Lesematerial (Offenes Buch)'},

    // Verwaltung & Behörden
    {id: 'bank', def: AccountBalanceOutlinedIcon, label: 'Behörde (Säulenbau)'},
    {id: 'law', def: GavelOutlinedIcon, label: 'Recht (Richterhammer)'},
    {id: 'public', def: PublicOutlinedIcon, label: 'Öffentlichkeit (Globus)'},
    {id: 'house', def: HomeOutlinedIcon, label: 'Wohnort (Haus)'},
    {id: 'factory', def: FactoryOutlinedIcon, label: 'Industrie (Fabrik)'},
    {id: 'business', def: StorefrontOutlinedIcon, label: 'Gewerbe (Ladengeschäft)'},
    {id: 'construction', def: ConstructionOutlinedIcon, label: 'Baustelle (Bauhelm)'},

    // Ideen & Aufgaben
    {id: 'lightbulb', def: LightbulbCircleOutlinedIcon, label: 'Idee (Glühbirne)'},
    {id: 'checklist', def: ChecklistOutlinedIcon, label: 'Aufgabenliste (Checkliste)'},
    {id: 'bulletList', def: FormatListBulletedOutlinedIcon, label: 'Aufzählung (Punktliste)'},
    {id: 'pen', def: EditOutlinedIcon, label: 'Bearbeiten (Stift)'},
    {id: 'build', def: BuildCircleOutlinedIcon, label: 'Werkzeug (Zahnrad mit Schraubenschlüssel)'},

    // Bildung & Arbeit
    {id: 'education', def: SchoolOutlinedIcon, label: 'Bildung (Absolventenmütze)'},
    {id: 'job', def: WorkOutlineOutlinedIcon, label: 'Beruf (Aktenkoffer)'},

    // Finanzen
    {id: 'euro', def: EuroOutlinedIcon, label: 'Euro (Währungssymbol)'},
    {id: 'payment', def: PaymentOutlinedIcon, label: 'Zahlung (Kreditkarte)'},
    {id: 'receipt', def: ReceiptLongOutlinedIcon, label: 'Beleg (Quittung)'},

    // Verkehr & Mobilität
    {id: 'car', def: DirectionsCarFilledOutlinedIcon, label: 'Auto (Fahrzeug)'},
    {id: 'shipping', def: LocalShippingOutlinedIcon, label: 'Lieferung (LKW)'},
    {id: 'mobile', def: MobileOutlinedIcon, label: 'Mobilgerät (Smartphone)'},

    // Sonstiges
    {id: 'pets', def: PetsOutlinedIcon, label: 'Tiere (Pfote)'},
    {id: 'cloud', def: CloudCircleOutlinedIcon, label: 'Cloud (Wolke)'},
    {id: 'flag', def: FlagCircleOutlinedIcon, label: 'Markierung (Flagge)'},
    {id: 'star', def: StarsOutlinedIcon, label: 'Favorit (Stern)'},
    {id: 'plus', def: AddCircleOutlineOutlinedIcon, label: 'Hinzufügen (Pluszeichen)'},
    {id: 'bell', def: CircleNotificationsOutlinedIcon, label: 'Benachrichtigung (Glocke)'},
    {id: 'heart', def: FavoriteBorderOutlinedIcon, label: 'Merken (Herz)'},
];

export const StepIconsMap = StepIcons.reduce<any>((acc, val) => ({
    ...acc,
    [val.id]: val,
}), {});

export function getStepIcon(step: StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement | SubmittedStepElement): SvgIconComponent {
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

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
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import FamilyRestroomOutlinedIcon from '@mui/icons-material/FamilyRestroomOutlined';
import ChecklistOutlinedIcon from '@mui/icons-material/ChecklistOutlined';
import FormatListBulletedOutlinedIcon from '@mui/icons-material/FormatListBulletedOutlined';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import FactoryOutlinedIcon from '@mui/icons-material/FactoryOutlined';
import EscalatorWarningOutlinedIcon from '@mui/icons-material/EscalatorWarningOutlined';
import EuroOutlinedIcon from '@mui/icons-material/EuroOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined';
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined';
import ChangeCircleOutlinedIcon from '@mui/icons-material/ChangeCircleOutlined';
import SwapHorizontalCircleOutlinedIcon from '@mui/icons-material/SwapHorizontalCircleOutlined';
import OfflineBoltOutlinedIcon from '@mui/icons-material/OfflineBoltOutlined';
import GavelOutlinedIcon from '@mui/icons-material/GavelOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import PaymentOutlinedIcon from '@mui/icons-material/PaymentOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import WorkOutlineOutlinedIcon from '@mui/icons-material/WorkOutlineOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import PetsOutlinedIcon from '@mui/icons-material/PetsOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import StorefrontOutlinedIcon from '@mui/icons-material/StorefrontOutlined';
import PublicOutlinedIcon from '@mui/icons-material/PublicOutlined';
import PhoneAndroidOutlinedIcon from '@mui/icons-material/PhoneAndroidOutlined';
import ConstructionOutlinedIcon from '@mui/icons-material/ConstructionOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import QuestionAnswerOutlinedIcon from '@mui/icons-material/QuestionAnswerOutlined';
import VerifiedUserOutlinedIcon from '@mui/icons-material/VerifiedUserOutlined';
import HowToRegOutlinedIcon from '@mui/icons-material/HowToRegOutlined';

export const StepIcons: StepIcon[] = [
    // Allgemein / Navigation
    {id: 'arrow', def: ArrowCircleRightOutlinedIcon, label: 'Weiter (Pfeil)'},
    {id: 'info', def: InfoOutlinedIcon, label: 'Information (i-Symbol)'},
    {id: 'question', def: HelpOutlineOutlinedIcon, label: 'Frage (Fragezeichen)'},
    {id: 'warning', def: ErrorOutlineOutlinedIcon, label: 'Warnung (Ausrufezeichen)'},
    {id: 'check', def: CheckCircleOutlineOutlinedIcon, label: 'Bestätigung (Haken)'},
    {id: 'change', def: ChangeCircleOutlinedIcon, label: 'Änderung (Kreis mit Pfeilen)'},
    {id: 'swap', def: SwapHorizontalCircleOutlinedIcon, label: 'Tausch (Wechselpfeile)'},
    {id: 'bolt', def: OfflineBoltOutlinedIcon, label: 'Schnellaktion (Blitz)'},
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
    {id: 'mobile', def: PhoneAndroidOutlinedIcon, label: 'Mobilgerät (Smartphone)'},

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

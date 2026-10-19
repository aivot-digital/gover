import {type StepIcon} from '../../../models/step-icon';
import Link from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import Monitoring from '@aivot/mui-material-symbols-400-n25-outlined/Monitoring';
import MenuBook from '@aivot/mui-material-symbols-400-n25-outlined/MenuBook';
import SupportAgent from '@aivot/mui-material-symbols-400-n25-outlined/SupportAgent';
import Language from '@aivot/mui-material-symbols-400-n25-outlined/Language';
import CorporateFare from '@aivot/mui-material-symbols-400-n25-outlined/CorporateFare';
import Apps from '@aivot/mui-material-symbols-400-n25-outlined/Apps';
import ContactMail from '@aivot/mui-material-symbols-400-n25-outlined/ContactMail';
import ContactPhone from '@aivot/mui-material-symbols-400-n25-outlined/ContactPhone';
import CalendarMonth from '@aivot/mui-material-symbols-400-n25-outlined/CalendarMonth';
import Cloud from '@aivot/mui-material-symbols-400-n25-outlined/Cloud';
import Public from '@aivot/mui-material-symbols-400-n25-outlined/Public';
import {type SvgIconComponent} from '../../../types/svg-icon-component';

export const CustomLinkIcons: StepIcon[] = [
    {id: 'link', def: Link, label: 'Allgemeiner Link'},
    {id: 'status', def: Monitoring, label: 'Status und Verfügbarkeit'},
    {id: 'documentation', def: MenuBook, label: 'Dokumentation und Anleitungen'},
    {id: 'support', def: SupportAgent, label: 'Support und Hilfe'},
    {id: 'website', def: Language, label: 'Webseite'},
    {id: 'intranet', def: CorporateFare, label: 'Intranet und Organisation'},
    {id: 'portal', def: Apps, label: 'Portal und Anwendungen'},
    {id: 'email', def: ContactMail, label: 'E-Mail und Kontakt'},
    {id: 'phone', def: ContactPhone, label: 'Telefon und Kontakt'},
    {id: 'calendar', def: CalendarMonth, label: 'Kalender und Termine'},
    {id: 'cloud', def: Cloud, label: 'Cloud-Dienst'},
    {id: 'public', def: Public, label: 'Öffentlicher Dienst'},
];

export const CustomLinkIconsMap = new Map(CustomLinkIcons.map((icon) => [icon.id, icon]));

export function getCustomLinkIcon(iconId?: string | null): SvgIconComponent {
    return CustomLinkIconsMap.get(iconId ?? '')?.def ?? Link;
}

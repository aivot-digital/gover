import {type SvgIconComponent} from '@mui/icons-material';
import HttpOutlinedIcon from '@mui/icons-material/HttpOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import ShareOutlinedIcon from '@mui/icons-material/ShareOutlined';

export enum DestinationType {
    Mail = 'Mail',
    HTTP = 'HTTP',
    Script = 'Script',
    OZGCloud = 'OZGCloud',
}

export const DestinationTypeIcons: Record<DestinationType, SvgIconComponent> = {
    [DestinationType.Mail]: EmailOutlinedIcon,
    [DestinationType.HTTP]: HttpOutlinedIcon,
    [DestinationType.Script]: HttpOutlinedIcon, // Placeholder, replace with actual icon if available
    [DestinationType.OZGCloud]: ShareOutlinedIcon,
};

export const DestinationTypeLabels: Record<DestinationType, string> = {
    [DestinationType.Mail]: 'Mail-Schnittstelle',
    [DestinationType.HTTP]: 'HTTP-Schnittstelle',
    [DestinationType.Script]: 'Script-Schnittstelle',
    [DestinationType.OZGCloud]: 'OZG-Cloud-Schnittstelle',
};

export const DestinationTypeOptions = [
    DestinationType.Mail,
    DestinationType.HTTP,
    DestinationType.OZGCloud,
    DestinationType.Script,
];

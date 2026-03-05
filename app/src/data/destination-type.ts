import {type SvgIconComponent} from '@mui/icons-material';
import HttpOutlinedIcon from '@mui/icons-material/HttpOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import ShareOutlinedIcon from '@mui/icons-material/ShareOutlined';

export enum DestinationType {
    Mail = 'Mail',
    HTTP = 'HTTP',
    OZGCloud = 'OZGCloud',
}

export const DestinationTypeIcons: Record<DestinationType, SvgIconComponent> = {
    [DestinationType.Mail]: EmailOutlinedIcon,
    [DestinationType.HTTP]: HttpOutlinedIcon,
    [DestinationType.OZGCloud]: ShareOutlinedIcon,
};

export const DestinationTypeLabels: Record<DestinationType, string> = {
    [DestinationType.Mail]: 'Mail-Schnittstelle',
    [DestinationType.HTTP]: 'HTTP-Schnittstelle',
    [DestinationType.OZGCloud]: 'OZG-Cloud-Schnittstelle',
};

export const DestinationTypeOptions = [
    DestinationType.Mail,
    DestinationType.HTTP,
    DestinationType.OZGCloud,
];

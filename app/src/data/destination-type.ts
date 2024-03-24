import HttpOutlinedIcon from '@mui/icons-material/HttpOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import {type SvgIconComponent} from '@mui/icons-material';

export enum DestinationType {
    Mail = 'Mail',
    HTTP = 'HTTP',
}

export const DestinationTypeIcons: Record<DestinationType, SvgIconComponent> = {
    [DestinationType.Mail]: EmailOutlinedIcon,
    [DestinationType.HTTP]: HttpOutlinedIcon,
};

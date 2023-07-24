import HttpOutlinedIcon from '@mui/icons-material/HttpOutlined';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';

export enum DestinationType {
    Mail = 'Mail',
    HTTP = 'HTTP',
}

export const DestinationTypeIcons = {
    [DestinationType.Mail]: EmailOutlinedIcon,
    [DestinationType.HTTP]: HttpOutlinedIcon,
};

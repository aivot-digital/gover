import {faCode, faEnvelope} from "@fortawesome/pro-light-svg-icons";

export enum DestinationType {
    Mail = 'Mail',
    HTTP = 'HTTP',
}

export const DestinationTypeIcons = {
    [DestinationType.Mail]: faEnvelope,
    [DestinationType.HTTP]: faCode,
}
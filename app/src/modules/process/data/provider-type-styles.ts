import {type SvgIconComponent} from '../../../types/svg-icon-component';
import {ProcessNodeType} from "../services/process-node-provider-api-service";
import Bolt from '@aivot/mui-material-symbols-400-n25-outlined/Bolt';
import Token from '@aivot/mui-material-symbols-400-n25-outlined/Token';
import ForkLeft from '@aivot/mui-material-symbols-400-n25-outlined/ForkLeft';
import Flag from '@aivot/mui-material-symbols-400-n25-outlined/Flag';

export const ProviderTypeStyles: Record<string, {
    Icon: SvgIconComponent;
    label: string;
    bgColor: string;
    textColor: string;
}> = {
    [ProcessNodeType.Trigger]: {
        Icon: Bolt,
        label: 'Auslöser',
        bgColor: '#D7E3FF',
        textColor: '#2C287D',
    },
    [ProcessNodeType.Action]: {
        Icon: Token,
        label: 'Aktion',
        bgColor: '#BAF8D7',
        textColor: '#024A37',
    },
    [ProcessNodeType.FlowControl]: {
        Icon: ForkLeft,
        label: 'Flusselement',
        bgColor: '#FFE1A6',
        textColor: '#674709',
    },
    [ProcessNodeType.Termination]: {
        Icon: Flag,
        label: 'Abschluss',
        bgColor: '#D7E3FF',
        textColor: '#2C287D',
    },
}

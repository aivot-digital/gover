import {FC} from 'react';
import {EmptyProcessTemplate} from './empty-process-template';
import Automation from '@aivot/mui-material-symbols-400-outlined/dist/automation/Automation';
import {ApiProcessTemplate} from './api-process-template';
import {SvgIconProps} from '@mui/material';
import Webhook from '@aivot/mui-material-symbols-400-outlined/dist/webhook/Webhook';
import {ProcessExport} from '../../entities/process-export';

interface ProcessTemplate {
    name: string;
    description: string;
    Icon: FC<SvgIconProps>;
    data: ProcessExport;
}

export const ProcessTemplates: ProcessTemplate[] = [
    {
        name: 'API-gesteuertes Verfahren',
        description: 'Ein Verfahren, das über eine API Schnittstelle gesteuert wird.',
        Icon: Webhook,
        data: ApiProcessTemplate,
    },
    {
        name: 'Leeres Verfahren',
        description: 'Ein leeres Verfahren ohne vordefinierte Schritte oder Logik.',
        Icon: Automation,
        data: EmptyProcessTemplate,
    },
];
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import RemoveRedEyeOutlinedIcon from '@mui/icons-material/RemoveRedEyeOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';

import {ReactElement} from 'react';
import DraftOrders from '@aivot/mui-material-symbols-400-outlined/dist/draft-orders/DraftOrders';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import Inventory2 from '@aivot/mui-material-symbols-400-outlined/dist/inventory-2/Inventory2';

export enum FormStatus {
    Drafted = 0,
    Published = 1,
    Revoked = 2,
}

export const FormStatusLabels: Record<FormStatus, string> = {
    [FormStatus.Drafted]: 'In Bearbeitung',
    [FormStatus.Published]: 'Veröffentlicht',
    [FormStatus.Revoked]: 'Zurückgezogen',
};

export const FormStatusDescriptions: Record<FormStatus, string> = {
    [FormStatus.Drafted]: 'Das Formular befindet sich im Entwurfsmodus und ist für Kunden nicht zugänglich.',
    [FormStatus.Published]: 'Das Formular ist veröffentlicht und für Kunden zugänglich.',
    [FormStatus.Revoked]: 'Das Formular wurde zurückgezogen und ist für Kunden nicht mehr zugänglich.',
};

export const FormStatusColors: Record<FormStatus, 'success' | 'warning' | 'error' | 'info'> = {
    [FormStatus.Drafted]: 'info',
    [FormStatus.Published]: 'success',
    [FormStatus.Revoked]: 'warning',
};

export const FormStatusIcons: Record<FormStatus, ReactElement> = {
    [FormStatus.Drafted]: <PersonOutlineOutlinedIcon />,
    [FormStatus.Published]: <RemoveRedEyeOutlinedIcon />,
    [FormStatus.Revoked]: <SwapHorizOutlinedIcon />,
};

export const FormVersionStatusIcons: Record<FormStatus, ReactElement> = {
    [FormStatus.Drafted]: <DraftOrders />,
    [FormStatus.Published]: ModuleIcons.forms,
    [FormStatus.Revoked]: <Inventory2 />,
};

import {ReactNode} from 'react';
import GpsFixedOutlinedIcon from '@mui/icons-material/GpsFixedOutlined';
import FunctionsOutlinedIcon from '@mui/icons-material/FunctionsOutlined';
import AbcOutlinedIcon from '@mui/icons-material/AbcOutlined';

export const OperandTypeIcon: Record<'value' | 'reference' | 'exp', ReactNode> = {
    exp: <FunctionsOutlinedIcon/>,
    reference: <GpsFixedOutlinedIcon/>,
    value: <AbcOutlinedIcon/>,
};
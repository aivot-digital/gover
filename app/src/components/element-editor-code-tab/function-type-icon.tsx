import {FunctionType} from './base-code-tab-props';
import CodeOutlinedIcon from '@mui/icons-material/CodeOutlined';
import DynamicFormOutlinedIcon from '@mui/icons-material/DynamicFormOutlined';
import {ReactNode} from 'react';

export const FunctionTypeIcon: Record<FunctionType, ReactNode> = {
    'legacy-code': <CodeOutlinedIcon/>,
    'legacy-condition': <DynamicFormOutlinedIcon/>,
    'code': <CodeOutlinedIcon/>,
    'expression': <DynamicFormOutlinedIcon/>,
};
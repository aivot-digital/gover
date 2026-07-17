import {FunctionType} from './base-code-tab-props';
import CodeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import DynamicFormOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/DynamicForm';
import {ReactNode} from 'react';

export const FunctionTypeIcon: Record<FunctionType, ReactNode> = {
    'legacy-code': <CodeOutlinedIcon/>,
    'legacy-condition': <DynamicFormOutlinedIcon/>,
    'code': <CodeOutlinedIcon/>,
    'expression': <DynamicFormOutlinedIcon/>,
};
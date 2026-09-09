import {FunctionType} from './base-code-tab-props';
import CodeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import {NoCodeIcon} from '../../modules/nocode/data/no-code-icon';
import {ReactNode} from 'react';

export const FunctionTypeIcon: Record<FunctionType, ReactNode> = {
    'legacy-code': <CodeOutlinedIcon/>,
    'legacy-condition': <NoCodeIcon/>,
    'code': <CodeOutlinedIcon/>,
    'expression': <NoCodeIcon/>,
};

import {ReactNode} from 'react';
import MyLocation from '@aivot/mui-material-symbols-400-n25-outlined/MyLocation';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import Article from '@aivot/mui-material-symbols-400-n25-outlined/Article';

export const OperandTypeIcon: Record<'value' | 'reference' | 'exp', ReactNode> = {
    exp: <Functions />,
    reference: <MyLocation />,
    value: <Article />,
};
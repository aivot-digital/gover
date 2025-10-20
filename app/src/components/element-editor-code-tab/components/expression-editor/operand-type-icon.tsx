import {ReactNode} from 'react';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import Article from '@aivot/mui-material-symbols-400-outlined/dist/article/Article';

export const OperandTypeIcon: Record<'value' | 'reference' | 'exp', ReactNode> = {
    exp: <Functions />,
    reference: <MyLocation />,
    value: <Article />,
};
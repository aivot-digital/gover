import * as React from 'react';
import {useTheme} from '@mui/material';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

const ShellDrawerUserIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => {
    const theme = useTheme();
    const maskId = useNormalizedReactId();
    const backgroundColor = theme.palette.mode === 'dark'
        ? theme.palette.grey[400]
        : theme.palette.grey[300];
    const foregroundColor = theme.palette.mode === 'dark'
        ? theme.palette.grey[700]
        : theme.palette.grey[600];

    return (
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width="30"
            height="30"
            fill="none"
            viewBox="0 0 30 30"
            {...props}
        >
            <circle cx="15" cy="15" r="15" fill={backgroundColor}/>
            <mask
                id={maskId}
                width="30"
                height="30"
                x="0"
                y="0"
                maskUnits="userSpaceOnUse"
                style={{maskType: 'alpha'}}
            >
                <circle cx="15" cy="15" r="15" fill={theme.palette.common.white}/>
            </mask>
            <g mask={`url(#${maskId})`}>
                <path
                    fill={foregroundColor}
                    d="M18.656-13.7 33.75 1.515V36.7h-37.5v-50.4zM21.25-20h-25C-7.187-20-10-17.165-10-13.7v50.4c0 3.465 2.813 6.3 6.25 6.3h37.5c3.438 0 6.25-2.835 6.25-6.3V-1.1zM15 17.8c3.438 0 6.25-2.835 6.25-6.3S18.438 5.2 15 5.2c-3.437 0-6.25 2.835-6.25 6.3s2.813 6.3 6.25 6.3m12.5 10.805c0-2.552-1.5-4.82-3.812-5.828a21.57 21.57 0 0 0-17.375 0A6.34 6.34 0 0 0 2.5 28.605V30.4h25z"
                />
            </g>
        </svg>
    );
};

export default ShellDrawerUserIcon;

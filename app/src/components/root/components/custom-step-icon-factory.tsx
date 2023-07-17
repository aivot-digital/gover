import React from 'react';
import {useTheme} from '@mui/material';
import {faArrowCircleRight} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';

export function CustomStepIconFactory(iconDef?: IconDefinition) {
    return (props: any) => {
        // eslint-disable-next-line react-hooks/rules-of-hooks
        const theme = useTheme();
        const {active} = props;
        const icon = iconDef ?? faArrowCircleRight;

        return (
            <FontAwesomeIcon
                fixedWidth
                icon={icon}
                size="2x"
                color={active ? theme.palette.primary.main : 'rgba(0, 0, 0, 0.4)'}
            />
        );
    };
}

import {RichtextElement} from '../../models/elements/form/content/richtext-element';
import {Typography} from '@mui/material';
import {BaseViewProps} from '../../views/base-view';
import React from 'react';

export function RichtextComponentView({element}: BaseViewProps<RichtextElement, void>) {
    return (
        <Typography
            component={'div'}
            variant="body2"
            className={'richtext-component-content content-without-margin-on-childs'}
            sx={{my: '1rem', maxWidth: '660px'}}
            dangerouslySetInnerHTML={{__html: element.content ?? ''}}
        />
    );
}


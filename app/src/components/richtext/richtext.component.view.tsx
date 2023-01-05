import {RichtextElement} from '../../models/elements/form-elements/content-elements/richtext-element';
import {Typography} from '@mui/material';
import {BaseViewProps} from '../_lib/base-view-props';

export function RichtextComponentView({element}: BaseViewProps<RichtextElement, void>) {
    return (
        <Typography
            variant="body2"
            dangerouslySetInnerHTML={{__html: element.content ?? ''}}
        />
    );
}


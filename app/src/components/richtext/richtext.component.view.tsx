import {RichtextElement} from '../../models/elements/form/content/richtext-element';
import {Typography} from '@mui/material';
import {BaseViewProps} from "../../views/base-view";

export function RichtextComponentView({element}: BaseViewProps<RichtextElement, void>) {
    return (
        <Typography
            variant="body2"
            dangerouslySetInnerHTML={{__html: element.content ?? ''}}
        />
    );
}


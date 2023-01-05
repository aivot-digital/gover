import {Box} from '@mui/material';
import {BaseViewProps} from '../_lib/base-view-props';
import {ImageElement} from '../../models/elements/form-elements/content-elements/image-element';

export function ImageView({element}: BaseViewProps<ImageElement, void>) {
    return (
        <Box>
            <img
                src={element.src}
                alt={element.alt}
            />
        </Box>
    );
}

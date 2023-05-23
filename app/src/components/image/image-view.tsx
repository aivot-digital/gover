import {Box} from '@mui/material';
import {ImageElement} from '../../models/elements/form/content/image-element';
import {BaseViewProps} from "../../views/base-view";

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

import {Box, Typography} from '@mui/material';
import {ImageElement} from '../../models/elements/form/content/image-element';
import {BaseViewProps} from "../../views/base-view";

export function ImageView({element}: BaseViewProps<ImageElement, void>) {
    return (
        <Box component={"figure"} sx={{m: 0}}>
            <img
                src={element.src}
                alt={element.alt}
            />
            {element.caption && (
                <Typography component={"figcaption"} variant="caption" color="text.secondary" display="block" mt={.5}>
                    {element.caption}
                </Typography>
            )}
        </Box>
    );
}

import {useTheme} from '@mui/material';
import {HeadlineElement} from '../../models/elements/form/content/headline-element';
import {BaseViewProps} from "../../views/base-view";
import {HeadlineComponent} from "./headline-component";

export function HeadlineComponentView({element}: BaseViewProps<HeadlineElement, void>) {
    const theme = useTheme();
    return (
        <HeadlineComponent
            small={element.small ?? false}
            content={element.content ?? ''}
        />
    );
}

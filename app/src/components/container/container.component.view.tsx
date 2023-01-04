import {ContainerElement} from '../../models/elements/form-elements/layout-elements/container-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {BaseViewProps} from '../_lib/base-view-props';
import {Grid} from '@mui/material';

export function ContainerComponentView({element, idPrefix}: BaseViewProps<ContainerElement, void>) {
    return (
        <Grid container spacing={2}>
            {
                (element.children ?? []).map((child, index) => (
                    <ViewDispatcherComponent
                        key={index}
                        model={child}
                        idPrefix={idPrefix}
                    />
                ))
            }
        </Grid>
    );
}


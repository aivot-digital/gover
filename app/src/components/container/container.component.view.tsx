import {GroupLayout} from '../../models/elements/form/layout/group-layout';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {BaseViewProps} from '../_lib/base-view-props';
import {Grid} from '@mui/material';

export function ContainerComponentView({element, idPrefix}: BaseViewProps<GroupLayout, void>) {
    return (
        <Grid container spacing={2}>
            {
                (element.children ?? []).map((child, index) => (
                    <ViewDispatcherComponent
                        key={index}
                        element={child}
                        idPrefix={idPrefix}
                    />
                ))
            }
        </Grid>
    );
}


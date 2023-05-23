import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Grid} from '@mui/material';
import {ContainerComponentProps} from "./container-component-props";

export function ContainerComponent({children, idPrefix}: ContainerComponentProps) {
    return (
        <Grid
            container
            spacing={2}
        >
            {
                children.map((child, index) => (
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


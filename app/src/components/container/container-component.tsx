import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {Grid} from '@mui/material';
import {ContainerComponentProps} from "./container-component-props";

export function ContainerComponent({allElements, children, idPrefix}: ContainerComponentProps) {
    return (
        <Grid
            container
            spacing={2}
        >
            {
                children.map((child, index) => (
                    <ViewDispatcherComponent
                        allElements={allElements}
                        key={index}
                        element={child}
                        idPrefix={idPrefix}
                    />
                ))
            }
        </Grid>
    );
}


import {BaseEditorProps} from './base-editor';
import {MapPointFieldElement} from '../models/elements/form/input/map-point-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {AlertComponent} from '../components/alert/alert-component';

export function MapPointFieldEditor(props: BaseEditorProps<MapPointFieldElement, ElementTreeEntity>) {
    const {
        element,
        onPatch,
        editable,
    } = props;

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12}}>
                <AlertComponent
                    color="warning"
                    sx={{my: 0}}
                    title="Technische Preview"
                    text={
                        'Das Kartenpunkt-Element befindet sich aktuell in einer technischen Preview.\n' +
                        'Ein produktiver Einsatz wird derzeit nicht empfohlen.\n\n' +
                        'Die aktuelle Implementierung nutzt öffentliche OpenStreetMap- und Nominatim-Dienste. ' +
                        'Bitte beachten Sie insbesondere mögliche Auswirkungen auf Datenschutz, Verfügbarkeit und Rate Limits.'
                    }
                />
            </Grid>
            <Grid size={{xs: 12, lg: 4}}>
                <NumberFieldComponent
                    label="Start-Breitengrad"
                    value={element.centerLatitude ?? 52.52}
                    onChange={(value) => {
                        onPatch({
                            centerLatitude: value,
                        });
                    }}
                    hint="Wird als Kartenzentrum verwendet, solange noch kein Punkt gesetzt ist."
                    disabled={!editable}
                    decimalPlaces={6}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 4}}>
                <NumberFieldComponent
                    label="Start-Längengrad"
                    value={element.centerLongitude ?? 13.405}
                    onChange={(value) => {
                        onPatch({
                            centerLongitude: value,
                        });
                    }}
                    hint="Wird als Kartenzentrum verwendet, solange noch kein Punkt gesetzt ist."
                    disabled={!editable}
                    decimalPlaces={6}
                />
            </Grid>
            <Grid size={{xs: 12, lg: 4}}>
                <NumberFieldComponent
                    label="Start-Zoomstufe"
                    value={element.zoom ?? 14}
                    onChange={(value) => {
                        onPatch({
                            zoom: value == null ? undefined : Math.min(19, Math.max(1, Math.round(value))),
                        });
                    }}
                    hint="Legt die initiale Zoomstufe für die Kartenansicht fest (1 bis 19)."
                    disabled={!editable}
                    decimalPlaces={0}
                />
            </Grid>
        </Grid>
    );
}

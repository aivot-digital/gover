import React, {useEffect, useState} from 'react';
import {Grid, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {type Department} from '../../modules/departments/models/department';
import {TextFieldComponent} from '../text-field/text-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {Form as Application} from '../../models/entities/form';
import {useApi} from "../../hooks/use-api";
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';

export function RootComponentEditorTabLegal(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [departments, setDepartments] = useState<Department[]>([]);

    useEffect(() => {
        new DepartmentsApiService(api)
            .list(0, 999, undefined, undefined, {
                ignoreMemberships: true,
            })
            .then(deps => setDepartments(deps.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Fachbereiche konnte nicht geladen werden'));
            });
    }, []);

    const departmentOptions = departments.map((department) => ({
        value: department.id.toString(),
        label: department.name,
    }));

    return (
        <>
            <Typography
                variant="h6"
            >
                Rechtliche Angaben
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für das Impressum"
                        value={props.entity.imprintDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                imprintDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für die Datenschutzerklärung"
                        value={props.entity.privacyDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                privacyDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={4}
                >
                    <SelectFieldComponent
                        label="Text für die Erklärung der Barrierefreiheit"
                        value={props.entity.accessibilityDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                accessibilityDepartmentId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="caption"
                color={"text.secondary"}
            >
                Rechtstexte werden auf Fachbereichs-Ebene hinterlegt und verwaltet. Sie können hier die Fachbereiche auswählen, deren Texte Sie verwenden und anzeigen möchten.
            </Typography>

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Informationen zum Datenschutz
            </Typography>

            <TextFieldComponent
                value={props.element.privacyText ?? ''}
                label="Text für Datenschutz-Einwilligung in den Allgemeinen Informationen"
                multiline
                onChange={(val) => {
                    props.onPatch({
                        privacyText: val,
                    });
                }}
                disabled={!props.editable}
            />

            <Typography>
                Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten,
                umschließen Sie den entsprechenden Text für den Link mit {'{privacy}'} und {'{/privacy}'}.
            </Typography>

            <Typography
                sx={{
                    mt: 2,

                }}
            >
                Z.B.: <strong>Hier finden Sie die {'{privacy}Hinweise zum Datenschutz{/privacy}'}.</strong>
            </Typography>


            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                }}
            >
                Lösch- und Zugriffsfristen
            </Typography>

            <NumberFieldComponent
                label="Löschfrist in Wochen"
                hint="Die Zeit in Wochen, nach der abgeschlossene Anträge automatisiert gelöscht werden. Geben Sie 0 ein um Anträge nicht zu löschen."
                placeholder="2"
                value={props.entity.submissionDeletionWeeks ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        submissionDeletionWeeks: val,
                    });
                }}
                decimalPlaces={0}
                suffix="Wochen"
                error={props.entity.submissionDeletionWeeks != null && props.entity.submissionDeletionWeeks < 0 ? 'Bitte geben Sie eine Löschfrist ein, die größer oder gleich 0 ist.' : undefined}
                disabled={!props.editable}
            />

            <NumberFieldComponent
                label="Zugriffsfrist in Stunden"
                hint="Die Zeit in Stunden, in der Nutzer:innen noch auf die von Ihnen gestellten Anträge zugreifen und diese herunterladen können."
                placeholder="4"
                value={props.entity.customerAccessHours ?? undefined}
                onChange={(val) => {
                    props.onPatchEntity({
                        customerAccessHours: val,
                    });
                }}
                decimalPlaces={0}
                suffix="Stunden"
                error={props.entity.customerAccessHours != null && props.entity.customerAccessHours <= 0 ? 'Bitte geben Sie eine Zugriffsfrist ein, die größer als 0 ist.' : undefined}
                disabled={!props.editable}
            />
        </>
    );
}

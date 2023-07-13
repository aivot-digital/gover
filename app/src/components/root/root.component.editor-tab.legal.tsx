import React, { useEffect, useState } from 'react';
import { Typography } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { type Department } from '../../models/entities/department';
import { DepartmentsService } from '../../services/departments-service';
import { TextFieldComponent } from '../text-field/text-field-component';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { SelectFieldComponent } from '../select-field/select-field-component';
import { NumberFieldComponent } from '../number-field/number-field-component';
import { showErrorSnackbar } from '../../slices/snackbar-slice';

export function RootComponentEditorTabLegal(props: BaseEditorProps<RootElement>): JSX.Element {
    const dispatch = useAppDispatch();
    const [departments, setDepartments] = useState<Department[]>([]);

    useEffect(() => {
        DepartmentsService
            .list()
            .then(setDepartments)
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
                Rechtliches
            </Typography>

            <SelectFieldComponent
                label="Text für das Impressum"
                value={ props.application.imprintDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        imprintDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departmentOptions }
            />

            <SelectFieldComponent
                label="Text für die Datenschutzerklärung"
                value={ props.application.privacyDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        privacyDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departmentOptions }
            />

            <SelectFieldComponent
                label="Text für die Erklärung der Barrierefreiheit"
                value={ props.application.accessibilityDepartment?.toString() ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        accessibilityDepartment: val != null ? parseInt(val) : undefined,
                    });
                } }
                options={ departmentOptions }
            />

            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Informationen zum Datenschutz
            </Typography>

            <TextFieldComponent
                value={ props.element.privacyText ?? '' }
                label="Text für Datenschutz-Einwilligung in den Allgemeinen Informationen"
                multiline
                onChange={ (val) => {
                    props.onPatch({
                        privacyText: val,
                    });
                } }
            />

            <Typography>
                Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten,
                umschließen Sie den entsprechenden Text für den Link mit { '{privacy}' } und { '{/privacy}' }.
            </Typography>

            <Typography
                sx={ {
                    mt: 2,

                } }
            >
                Z.B.: <strong>Hier finden Sie die { '{privacy}Hinweise zum Datenschutz{/privacy}' }.</strong>
            </Typography>


            <Typography
                variant="h6"
                sx={ {
                    mt: 4,
                } }
            >
                Lösch- und Zugriffsfristen
            </Typography>

            <NumberFieldComponent
                label="Löschfrist in Wochen"
                hint="Die Zeit in Wochen, nach der abgeschlossene Anträge automatisiert gelöscht werden. Geben Sie 0 ein um Anträge nicht zu löschen."
                placeholder="2"
                value={ props.application.submissionDeletionWeeks ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        submissionDeletionWeeks: val,
                    });
                } }
                decimalPlaces={ 0 }
                suffix="Wochen"
            />

            <NumberFieldComponent
                label="Zugriffsfrist in Stunden"
                hint="Die Zeit in Stunden, in der Nutzer:innen noch auf die von Ihnen gestellten Anträge zugreifen und diese herunterladen können."
                placeholder="4"
                value={ props.application.customerAccessHours ?? undefined }
                onChange={ (val) => {
                    props.onPatchApplication({
                        customerAccessHours: val,
                    });
                } }
                decimalPlaces={ 0 }
                suffix="Stunden"
            />
        </>
    );
}

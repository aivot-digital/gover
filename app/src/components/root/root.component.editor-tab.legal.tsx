import React, { useEffect, useState } from 'react';
import { Typography } from '@mui/material';
import { type BaseEditorProps } from '../../editors/base-editor';
import { type RootElement } from '../../models/elements/root-element';
import { type Department } from '../../models/entities/department';
import { DepartmentsService } from '../../services/departments-service';
import { TextFieldComponent } from '../text-field/text-field-component';
import { useAppDispatch } from '../../hooks/use-app-dispatch';
import { useAppSelector } from '../../hooks/use-app-selector';
import { selectLoadedApplication, updateAppModel } from '../../slices/app-slice';
import { type Application } from '../../models/entities/application';
import { SelectFieldComponent } from '../select-field/select-field-component';
import { NumberFieldComponent } from '../number-field/number-field-component';

export function RootComponentEditorTabLegal(props: BaseEditorProps<RootElement>) {
    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const [departments, setDepartments] = useState<Department[]>([]);

    useEffect(() => {
        DepartmentsService
            .list()
            .then(setDepartments);
    }, []);

    const patchApplication = (patch: Partial<Application>) => {
        if (application == null) {
            return;
        }

        dispatch(updateAppModel({
            ...application,
            ...patch,
        }));
    };

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
                value={application?.imprintDepartment?.toString() ?? undefined}
                onChange={(val) => {
                    patchApplication({
                        imprintDepartment: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departmentOptions}
            />

            <SelectFieldComponent
                label="Text für die Datenschutzerklärung"
                value={application?.privacyDepartment?.toString() ?? undefined}
                onChange={(val) => {
                    patchApplication({
                        privacyDepartment: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departmentOptions}
            />

            <SelectFieldComponent
                label="Text für die Erklärung der Barrierefreiheit"
                value={application?.accessibilityDepartment?.toString() ?? undefined}
                onChange={(val) => {
                    patchApplication({
                        accessibilityDepartment: val != null ? parseInt(val) : undefined,
                    });
                }}
                options={departmentOptions}
            />

            <Typography
                variant="h6"
                sx={{ mt: 4 }}
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
            />

            <Typography>
                Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten,
                umschließen Sie den entsprechenden Text für den Link mit {'{privacy}'} und {'{/privacy}'}.
            </Typography>

            <Typography sx={{ mt: 2 }}>
                Z.B.: <strong>Hier finden Sie die {'{privacy}Hinweise zum Datenschutz{/privacy}'}.</strong>
            </Typography>


            <Typography
                variant="h6"
                sx={{ mt: 4 }}
            >
                Lösch- und Zugriffsfristen
            </Typography>

            <NumberFieldComponent
                label="Löschfrist in Wochen"
                hint="Die Zeit in Wochen, nach der abgeschlossene Anträge automatisiert gelöscht werden. Geben Sie -1 ein um Anträge nicht zu löschen."
                placeholder="2"
                value={application?.submissionDeletionWeeks ?? undefined}
                onChange={(val) => {
                    patchApplication({
                        submissionDeletionWeeks: val,
                    });
                }}
                decimalPlaces={0}
                suffix="Wochen"
            />

            <NumberFieldComponent
                label="Zugriffsfrist in Stunden"
                hint="Die Zeit in Stunden, in der Nutzer:innen noch auf die von Ihnen gestellten Anträge zugreifen und diese herunterladen können."
                placeholder="4"
                value={application?.customerAccessHours ?? undefined}
                onChange={(val) => {
                    patchApplication({
                        customerAccessHours: val,
                    });
                }}
                decimalPlaces={0}
                suffix="Stunden"
            />
        </>
    );
}

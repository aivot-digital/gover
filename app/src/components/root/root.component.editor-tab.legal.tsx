import React, {useEffect, useState} from 'react';
import {Grid, Skeleton, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {TextFieldComponent} from '../text-field/text-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {NumberFieldComponent} from '../number-field/number-field-component';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {withDelay} from '../../utils/with-delay';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {DepartmentEntity} from '../../modules/departments/entities/department-entity';
import {LoadedForm} from '../../slices/app-slice';

export function RootComponentEditorTabLegal(props: BaseEditorProps<RootElement, LoadedForm>) {
    const dispatch = useAppDispatch();
    const [departments, setDepartments] = useState<DepartmentEntity[]>([]);

    useEffect(() => {
        withDelay(
            new DepartmentApiService().listAll(),
            600,
        )
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

    if (departmentOptions.length === 0) {
        return EditorSkeleton;
    }

    return (
        <>
            <ElementEditorSectionHeader
                title="Rechtliche Angaben"
                disableMarginTop
            >
                Rechtstexte werden auf Fachbereichs-Ebene hinterlegt und verwaltet. Sie können hier die Fachbereiche auswählen, deren Texte Sie verwenden und anzeigen möchten.
            </ElementEditorSectionHeader>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Text für das Impressum"
                        value={props.entity.version.imprintDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                version: {
                                    ...props.entity.version,
                                    imprintDepartmentId: val != null ? parseInt(val) : null,
                                },
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Text für die Datenschutzerklärung"
                        value={props.entity.version.privacyDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                version: {
                                    ...props.entity.version,
                                    privacyDepartmentId: val != null ? parseInt(val) : null,
                                },
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Text für die Erklärung der Barrierefreiheit"
                        value={props.entity.version.accessibilityDepartmentId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                version: {
                                    ...props.entity.version,
                                    accessibilityDepartmentId: val != null ? parseInt(val) : null,
                                },
                            });
                        }}
                        options={departmentOptions}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Informationen zum Datenschutz"
                variant={'h5'}
            />
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
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

                    <Typography
                        variant={'caption'}
                        color={'text.secondary'}
                    >
                        Wenn Sie innerhalb der Informationen zum Datenschutz auf die Datenschutzerklärung verlinken möchten,
                        umschließen Sie den entsprechenden Text für den Link mit {'{privacy}'} und {'{/privacy}'}. Zum
                        Beispiel wie im Standard-Text: <i>Hier finden Sie die {'{privacy}Hinweise zum Datenschutz{/privacy}'}.</i>
                    </Typography>
                </Grid>
            </Grid>
            <ElementEditorSectionHeader
                title="Lösch- und Zugriffsfristen"
                variant={'h5'}
            />
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <NumberFieldComponent
                        label="Löschfrist in Wochen"
                        hint="Die Zeit in Wochen, nach der abgeschlossene Anträge automatisiert gelöscht werden. Geben Sie 0 ein um Anträge nicht zu löschen."
                        placeholder="2"
                        value={props.entity.version.submissionRetentionWeeks ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                version: {
                                    ...props.entity.version,
                                    submissionRetentionWeeks: val ?? null,
                                },
                            });
                        }}
                        decimalPlaces={0}
                        suffix="Wochen"
                        error={props.entity.version.submissionRetentionWeeks != null && props.entity.version.submissionRetentionWeeks < 0 ? 'Bitte geben Sie eine Löschfrist ein, die größer oder gleich 0 ist.' : undefined}
                        disabled={!props.editable}
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <NumberFieldComponent
                        label="Zugriffsfrist in Stunden"
                        hint="Die Zeit in Stunden, in der Nutzer:innen noch auf die von Ihnen gestellten Anträge zugreifen und diese herunterladen können."
                        placeholder="4"
                        value={props.entity.version.customerAccessHours ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                version: {
                                    ...props.entity.version,
                                    customerAccessHours: val ?? null,
                                },
                            });
                        }}
                        decimalPlaces={0}
                        suffix="Stunden"
                        error={props.entity.version.customerAccessHours != null && props.entity.version.customerAccessHours <= 0 ? 'Bitte geben Sie eine Zugriffsfrist ein, die größer als 0 ist.' : undefined}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
        </>
    );
}

const EditorSkeleton = (
    <>
        <Skeleton
            width={200}
            height={30}
        />

        <Skeleton
            width={900}
            height={48}
        />

        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 4,
                }}
            >
                <Skeleton
                    width="100%"
                    height={80}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 4,
                }}
            >
                <Skeleton
                    width="100%"
                    height={80}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 4,
                }}
            >
                <Skeleton
                    width="100%"
                    height={80}
                />
            </Grid>
        </Grid>

        <Skeleton
            width={200}
            height={30}
        />

        <Skeleton
            width={248}
            height={200}
        />

        <Skeleton
            width={200}
            height={30}
        />

        <Grid
            container
            columnSpacing={4}
        >
            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <Skeleton
                    width="100%"
                    height={80}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <Skeleton
                    width="100%"
                    height={80}
                />
            </Grid>
        </Grid>
    </>
);

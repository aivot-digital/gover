import {BaseEditorProps} from './base-editor';
import {AssignmentContextFieldElement} from '../models/elements/form/input/assignment-context-field-element';
import {ElementTreeEntity} from '../components/element-tree/element-tree-entity';
import {Grid} from '@mui/material';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {NumberFieldComponent} from '../components/number-field/number-field-component';
import {
    DomainAndUserSelectItemType,
    DomainAndUserSelectItemTypes,
    DomainAndUserSelectProcessAccessConstraint,
} from '../models/elements/form/input/domain-user-select-field-element';
import {MultiCheckboxComponent} from '../components/multi-checkbox-field/multi-checkbox-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {StringListInput2} from '../components/string-list-input/string-list-input-2';

export function AssignmentContextFieldEditor(props: BaseEditorProps<AssignmentContextFieldElement, ElementTreeEntity>) {
    const {
        element,
        editable,
        onPatch,
    } = props;

    const minItemsError = (
        element.maxItems != null &&
        element.minItems != null &&
        element.minItems > element.maxItems
    );

    const maxItemsError = (
        element.maxItems != null &&
        element.minItems != null &&
        element.maxItems < element.minItems
    );

    const allowedTypes = element.allowedTypes ?? DomainAndUserSelectItemTypes;
    const noTypeEnabled = allowedTypes.length === 0;
    const processAccessConstraint = element.processAccessConstraint ?? undefined;
    const hasProcessAccessConstraint = processAccessConstraint != null;

    const createConstraintPatch = (
        patch: Partial<DomainAndUserSelectProcessAccessConstraint>,
    ): DomainAndUserSelectProcessAccessConstraint => {
        return {
            processId: patch.processId ?? processAccessConstraint?.processId ?? 1,
            processVersion: patch.processVersion ?? processAccessConstraint?.processVersion ?? 1,
            requiredPermissions: patch.requiredPermissions ?? processAccessConstraint?.requiredPermissions ?? undefined,
        };
    };

    return (
        <Grid
            container
            columnSpacing={4}
            rowSpacing={2}
        >
            <Grid size={{xs: 12}}>
                <TextFieldComponent
                    label="Überschrift"
                    value={element.headline}
                    onChange={(value) => {
                        onPatch({
                            headline: value,
                        });
                    }}
                    hint="Überschrift oberhalb des Personenkreis-Feldes."
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <TextFieldComponent
                    label="Beschreibungstext"
                    value={element.text}
                    onChange={(value) => {
                        onPatch({
                            text: value,
                        });
                    }}
                    multiline
                    hint="Kurzer Beschreibungstext unterhalb der Überschrift."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 6,
                }}
            >
                <TextFieldComponent
                    label="Platzhalter"
                    value={element.placeholder}
                    onChange={(value) => {
                        onPatch({
                            placeholder: value,
                        });
                    }}
                    hint="Der Platzhalter wird im Personenkreis-Eingabefeld angezeigt, solange noch keine Auswahl getroffen wurde."
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 3,
                }}
            >
                <NumberFieldComponent
                    label="Minimale Anzahl"
                    value={element.minItems ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            minItems: value,
                        });
                    }}
                    hint="Geben Sie 0 oder nichts ein, um keine Mindestanzahl zu fordern."
                    error={minItemsError ? 'Die Mindestanzahl darf nicht größer als die Maximalanzahl sein.' : undefined}
                    disabled={!editable}
                />
            </Grid>

            <Grid
                size={{
                    xs: 12,
                    lg: 3,
                }}
            >
                <NumberFieldComponent
                    label="Maximale Anzahl"
                    value={element.maxItems ?? undefined}
                    onChange={(value) => {
                        onPatch({
                            maxItems: value,
                        });
                    }}
                    hint="Lassen Sie das Feld leer, wenn keine Obergrenze gelten soll."
                    error={maxItemsError ? 'Die Maximalanzahl darf nicht kleiner als die Mindestanzahl sein.' : undefined}
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <MultiCheckboxComponent
                    label="Auswählbare Typen"
                    value={allowedTypes}
                    onChange={(value) => {
                        onPatch({
                            allowedTypes: (value ?? []) as DomainAndUserSelectItemType[],
                        });
                    }}
                    options={[
                        {
                            value: 'orgUnit',
                            label: 'Organisationseinheiten',
                        },
                        {
                            value: 'team',
                            label: 'Teams',
                        },
                        {
                            value: 'user',
                            label: 'Mitarbeitende',
                        },
                    ]}
                    hint={noTypeEnabled ? 'Derzeit ist kein Typ auswählbar.' : 'Wählen Sie, welche Typen im Feld zur Auswahl stehen.'}
                    disabled={!editable}
                />
            </Grid>

            <Grid size={{xs: 12}}>
                <CheckboxFieldComponent
                    label="Auswahl auf Prozessrechte beschränken"
                    value={hasProcessAccessConstraint}
                    onChange={(checked) => {
                        onPatch({
                            processAccessConstraint: checked ? createConstraintPatch({}) : undefined,
                        });
                    }}
                    hint="Wenn aktiviert, werden die Optionen anhand von Prozess-ID, Version und notwendigen Berechtigungen eingeschränkt."
                    disabled={!editable}
                />
            </Grid>

            {
                hasProcessAccessConstraint &&
                <>
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <NumberFieldComponent
                            label="Prozess-ID"
                            value={processAccessConstraint.processId}
                            onChange={(value) => {
                                onPatch({
                                    processAccessConstraint: createConstraintPatch({
                                        processId: value ?? processAccessConstraint.processId,
                                    }),
                                });
                            }}
                            minValue={1}
                            hint="Verfahrens-ID, für die die möglichen Personenkreise ermittelt werden."
                            disabled={!editable}
                        />
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            lg: 6,
                        }}
                    >
                        <NumberFieldComponent
                            label="Prozessversion"
                            value={processAccessConstraint.processVersion}
                            onChange={(value) => {
                                onPatch({
                                    processAccessConstraint: createConstraintPatch({
                                        processVersion: value ?? processAccessConstraint.processVersion,
                                    }),
                                });
                            }}
                            minValue={1}
                            hint="Version des Verfahrens, für die die Rechte ausgewertet werden."
                            disabled={!editable}
                        />
                    </Grid>

                    <Grid size={{xs: 12}}>
                        <StringListInput2
                            label="Notwendige Berechtigungen"
                            value={processAccessConstraint.requiredPermissions ?? undefined}
                            onChange={(value) => {
                                const normalizedPermissions = (value ?? [])
                                    .map((entry) => entry.trim())
                                    .filter((entry) => entry.length > 0);

                                onPatch({
                                    processAccessConstraint: createConstraintPatch({
                                        requiredPermissions: normalizedPermissions.length > 0 ? normalizedPermissions : undefined,
                                    }),
                                });
                            }}
                            allowEmpty
                            addLabel="Berechtigung hinzufügen"
                            noItemsHint="Es sind keine Berechtigungen hinterlegt."
                            hint="Optional: Nur passende Personen/Domänen mit diesen Berechtigungen werden angeboten. Das Wildcard-Recht '*' wird serverseitig berücksichtigt."
                            disabled={!editable}
                        />
                    </Grid>
                </>
            }
        </Grid>
    );
}

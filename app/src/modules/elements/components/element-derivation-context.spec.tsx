import {describe, expect, it, vi} from 'vitest';
import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ElementType} from '../../../data/element-type/element-type';
import type {
    AuthoredElementValues,
    ComputedElementErrors,
    DerivedRuntimeElementData,
} from '../../../models/element-data';
import {
    createDerivedRuntimeElementData,
} from '../../../models/element-data';
import {ElementDerivationContext} from './element-derivation-context';

vi.mock('../../../hooks/use-app-dispatch', () => ({
    useAppDispatch: () => vi.fn(),
}));

vi.mock('../../../components/view-dispatcher/view-dispatcher.component', () => ({
    ViewDispatcherComponent: (props: any) => (
        <>
            <button
                type="button"
                onClick={() => props.onAuthoredElementValuesChange({field: 'valid'}, ['field'])}
            >
                Wert setzen
            </button>
            <button
                type="button"
                onClick={() => props.onDerive(props.authoredElementValues, [], [])}
            >
                Validieren
            </button>
            <button
                type="button"
                onClick={() => {
                    const currentRows = Array.isArray(props.authoredElementValues.rows) ? props.authoredElementValues.rows : [];
                    props.onAuthoredElementValuesChange({
                        ...props.authoredElementValues,
                        rows: [
                            ...currentRows,
                            {
                                id: `row-${currentRows.length + 1}`,
                                values: {},
                            },
                        ],
                    }, ['rows', 'rowField']);
                }}
            >
                Datensatz hinzufügen
            </button>
            <button
                type="button"
                onClick={() => {
                    const currentRows = Array.isArray(props.authoredElementValues.rows) ? props.authoredElementValues.rows : [];
                    props.onAuthoredElementValuesChange({
                        ...props.authoredElementValues,
                        rows: currentRows.map((row: any, index: number) => index === 0 ? {
                            ...row,
                            values: {
                                ...(row.values ?? {}),
                                rowField: 'valid',
                            },
                        } : row),
                    }, ['rows', 'rowField']);
                }}
            >
                Ersten Datensatz ändern
            </button>
            <output data-testid="field-error">
                {props.derivedData.elementStates.field?.error ?? ''}
            </output>
            <output data-testid="row-1-error">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-1')
                        ?.states?.rowField?.error ?? ''
                }
            </output>
            <output data-testid="row-2-error">
                {
                    props.derivedData.elementStates.rows?.subStates
                        ?.find((subState: any) => subState.id === 'row-2')
                        ?.states?.rowField?.error ?? ''
                }
            </output>
        </>
    ),
}));

describe('ElementDerivationContext', () => {
    it('should not persist external computed errors when authored values change', async () => {
        const onAuthoredElementValuesChange = vi.fn();
        const onDerivedDataChange = vi.fn();
        const computedErrors: ComputedElementErrors = {
            field: {
                error: 'Der Verantwortliche Personenkreis ist ein Pflichtfeld.',
            },
        };

        render(
            <ElementDerivationContext
                element={createRootElement()}
                authoredElementValues={{field: null}}
                onAuthoredElementValuesChange={onAuthoredElementValuesChange}
                onDerivedDataChange={onDerivedDataChange}
                computedErrors={computedErrors}
                onDeriveOverride={() => Promise.resolve(createDerivedRuntimeElementData())}
            />,
        );

        await waitFor(() => expect(onDerivedDataChange).toHaveBeenCalled());
        expect(screen.getByTestId('field-error')).toHaveTextContent(computedErrors.field?.error as string);
        onDerivedDataChange.mockClear();

        fireEvent.click(screen.getByRole('button', {name: 'Wert setzen'}));

        expect(onAuthoredElementValuesChange).toHaveBeenCalledWith({field: 'valid'});
        const patchedDerivedData = onDerivedDataChange.mock.calls[0][0] as DerivedRuntimeElementData;
        expect(patchedDerivedData.elementStates.field?.error).toBeUndefined();
        await waitFor(() => expect(screen.getByTestId('field-error')).toBeEmptyDOMElement());
    });

    it('should preserve sibling row errors while suppressing only the changed row until explicit revalidation', async () => {
        const onDerivedDataChange = vi.fn();
        const validationError = 'Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.';
        let shouldReturnValidationErrors = true;
        const onDeriveOverride = vi.fn((authoredElementValues: AuthoredElementValues, skipErrorsForElements: string[]) => {
            const rows = Array.isArray(authoredElementValues.rows) ? authoredElementValues.rows : [];
            const shouldIncludeErrors = !skipErrorsForElements.includes('ALL') && shouldReturnValidationErrors;

            return Promise.resolve(createDerivedRuntimeElementData({
                effectiveValues: authoredElementValues,
                elementStates: {
                    field: {
                        error: shouldIncludeErrors ? validationError : null,
                    },
                    rows: {
                        subStates: rows.map((row: {id?: string | null}) => ({
                            id: row.id,
                            states: {
                                rowField: {
                                    error: shouldIncludeErrors ? `Fehler in ${row.id}` : null,
                                },
                            },
                        })),
                    },
                    dependent: {
                        visible: true,
                    },
                },
            }));
        });

        render(
            <ReplicatingContainerDerivationHarness
                onDerivedDataChange={onDerivedDataChange}
                onDeriveOverride={onDeriveOverride}
            />
        );

        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(1));

        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(screen.getByTestId('field-error')).toHaveTextContent(validationError));
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');

        fireEvent.click(screen.getByRole('button', {name: 'Datensatz hinzufügen'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(3));
        expect(onDeriveOverride).toHaveBeenLastCalledWith(
            expect.objectContaining({
                rows: [
                    {
                        id: 'row-1',
                        values: {
                            rowField: null,
                        },
                    },
                    {
                        id: 'row-2',
                        values: {},
                    },
                ],
            }),
            ['ALL'],
        );
        expect(screen.getByTestId('field-error')).toHaveTextContent(validationError);
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');
        expect(screen.getByTestId('row-2-error')).toBeEmptyDOMElement();

        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(4));
        expect(screen.getByTestId('row-1-error')).toHaveTextContent('Fehler in row-1');
        expect(screen.getByTestId('row-2-error')).toHaveTextContent('Fehler in row-2');

        fireEvent.click(screen.getByRole('button', {name: 'Ersten Datensatz ändern'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(5));
        expect(screen.getByTestId('row-1-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-2-error')).toHaveTextContent('Fehler in row-2');

        shouldReturnValidationErrors = false;
        fireEvent.click(screen.getByRole('button', {name: 'Validieren'}));
        await waitFor(() => expect(onDeriveOverride).toHaveBeenCalledTimes(6));
        expect(screen.getByTestId('field-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-1-error')).toBeEmptyDOMElement();
        expect(screen.getByTestId('row-2-error')).toBeEmptyDOMElement();
    });
});

interface ReplicatingContainerDerivationHarnessProps {
    onDerivedDataChange: (derivedData: DerivedRuntimeElementData) => void;
    onDeriveOverride: (authoredElementValues: AuthoredElementValues, skipErrorsForElements: string[]) => Promise<DerivedRuntimeElementData>;
}

function ReplicatingContainerDerivationHarness(props: ReplicatingContainerDerivationHarnessProps) {
    const {
        onDerivedDataChange,
        onDeriveOverride,
    } = props;
    const element = React.useMemo(() => createRootElementWithReplicatingContainer(), []);
    const [authoredElementValues, setAuthoredElementValues] = React.useState<AuthoredElementValues>({
        field: null,
        rows: [
            {
                id: 'row-1',
                values: {
                    rowField: null,
                },
            },
        ],
    });

    return (
        <ElementDerivationContext
            element={element}
            authoredElementValues={authoredElementValues}
            onAuthoredElementValuesChange={setAuthoredElementValues}
            onDerivedDataChange={onDerivedDataChange}
            onDeriveOverride={onDeriveOverride}
        />
    );
}

function createRootElement(): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {
                id: 'field',
                type: ElementType.Text,
                disabled: false,
                technical: false,
            },
        ],
    };
}

function createRootElementWithReplicatingContainer(): any {
    return {
        id: 'root',
        type: ElementType.GroupLayout,
        children: [
            {
                id: 'field',
                type: ElementType.Text,
                disabled: false,
                technical: false,
            },
            {
                id: 'rows',
                type: ElementType.ReplicatingContainer,
                disabled: false,
                technical: false,
                children: [
                    {
                        id: 'rowField',
                        type: ElementType.Text,
                        disabled: false,
                        technical: false,
                    },
                ],
            },
            {
                id: 'dependent',
                type: ElementType.Text,
                visibility: {
                    type: 'NoCode',
                    referencedIds: ['rows'],
                },
            },
        ],
    };
}

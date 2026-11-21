import {useState} from 'react';
import {fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {FunctionInputView} from './function-input-view';
import {BaseViewProps} from './base-view';
import {
    FunctionInputElement,
    FunctionInputElementItem,
    FunctionInputElementItemType,
    FunctionInputElementReturnType,
} from '../models/elements/form/input/function-input-element';
import {generateElementWithDefaultValues} from '../utils/generate-element-with-default-values';
import {ElementType} from '../data/element-type/element-type';
import {ConditionSetOperator} from '../data/condition-set-operator';
import {ViewDispatcherContextProvider, ViewDispatcherMode} from '../components/view-dispatcher/view-dispatcher.context';
import {NoCodeDataType} from '../data/no-code-data-type';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../hooks/use-api', () => ({useApi: () => api}));
vi.mock('../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
const initial: FunctionInputElementItem = {
    type: FunctionInputElementItemType.ConditionSet,
    conditionSet: {operator: ConditionSetOperator.All},
    noCode: null,
    code: null,
};
const operator = {
    identifier: 'always-true',
    label: 'Immer wahr',
    packageName: 'core',
    tags: [],
    description: '',
    abstractDescription: '',
    humanReadableTemplate: null,
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [],
    }],
};

function setup(value: FunctionInputElementItem | null = initial, readOnly = false) {
    const onChange = vi.fn();
    const element = {
        ...(generateElementWithDefaultValues(ElementType.FunctionInput) as unknown as FunctionInputElement),
        label: 'Bedingung',
        returnType: FunctionInputElementReturnType.BOOLEAN,
    } as FunctionInputElement;
    const root = generateElementWithDefaultValues(ElementType.FormLayout);

    function Harness() {
        const [current, setCurrent] = useState(value);
        const props = {
            element,
            value: current,
            setValue: (next: FunctionInputElementItem | null) => {
                onChange(next);
                setCurrent(next);
            },
            isBusy: false,
            isDeriving: false,
        } as BaseViewProps<FunctionInputElement, FunctionInputElementItem>;
        return <ViewDispatcherContextProvider value={{
            mode: ViewDispatcherMode.Viewer,
            readOnly,
            rootElement: root,
            allElements: [element],
            rootAuthoredElementValues: {},
            rootDerivedData: {
                effectiveValues: {},
                elementStates: {},
            },
        }}>
            <FunctionInputView {...props}/>
        </ViewDispatcherContextProvider>;
    }

    render(<Harness/>);
    return onChange;
}

beforeEach(() => api.get.mockReset().mockResolvedValue([operator]));
describe('function input conversion', () => {
    it('only offers new No-Code and JavaScript for new functions', () => {
        setup(null);
        expect(screen.getAllByRole('button', {name: 'Funktion anlegen'})).toHaveLength(2);
        expect(screen.getByText('Low-Code – JavaScript')).toBeInTheDocument();
        expect(screen.getByText(/No-Code – Regeln und Berechnungen/)).toBeInTheDocument();
        fireEvent.click(screen.getAllByRole('button', {name: 'Funktion anlegen'})[0]);
        expect(screen.getByRole('button', {name: 'Bearbeiten'})).toBeInTheDocument();
    });
    it('keeps the old rule when cancelling and only replaces it when a valid new expression is applied', async () => {
        const onChange = setup();
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
        expect(within(screen.getByRole('dialog')).getByText('Bisherige Regel')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', {name: 'Abbrechen'}));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(onChange).not.toHaveBeenCalled();
        fireEvent.click(screen.getByRole('button', {name: 'Umstellung beginnen'}));
        expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeDisabled();
        fireEvent.click(await screen.findByRole('button', {name: 'Ausdruck hinzufügen'}));
        fireEvent.click(screen.getByRole('button', {name: 'Operator'}));
        fireEvent.click(within(await screen.findByRole('dialog', {name: 'Operator auswählen'})).getByRole('button', {name: 'Immer wahr: Hinzufügen'}));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Übernehmen'})).toBeEnabled());
        fireEvent.click(screen.getByRole('button', {name: 'Übernehmen'}));
        expect(onChange).toHaveBeenCalledWith({
            type: 'NoCode',
            noCode: {
                type: 'NoCodeExpression',
                operatorIdentifier: 'always-true',
                operands: [],
            },
            conditionSet: null,
            code: null,
        });
    });
    it('keeps legacy rules visible without offering conversion in read-only mode', () => {
        setup(initial, true);
        expect(screen.getByText(/Ab Prosuna 5.1/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Umstellung beginnen'})).not.toBeInTheDocument();
    });
});

import React, {useState} from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {type AnyElement} from '../../../models/elements/any-element';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {ElementTreeContextProvider, type ElementTreeContextType} from '../element-tree-context';
import {ElementTreeEditorContextProvider} from './element-tree-editor-context';
import {ElementTreeEditorContentTabProperties} from './element-tree-editor-content-tab-properties';

vi.mock('../../../hooks/use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));
vi.mock('../../../editors', async () => {
    const {StepComponentEditor} = await import('../../step/step.component.editor');
    const {ElementType} = await import('../../../data/element-type/element-type');

    // Keep the real section title editor; other type-specific controls are outside this shared-properties test.
    return {editors: {[ElementType.Step]: {default: StepComponentEditor}}};
});

describe('ElementTreeEditorContentTabProperties editor naming', () => {
    it.each([
        {
            kind: 'section',
            element: {...generateElementWithDefaultValues(ElementType.Step), title: 'Persönliche Angaben'},
            publicField: /^Titel des Abschnitts/,
            publicValue: 'Persönliche Angaben',
        },
        {
            kind: 'input',
            element: {...generateElementWithDefaultValues(ElementType.Text), label: 'Nachname'},
            publicField: /^Titel – optional$/,
            publicValue: 'Nachname',
        },
        {
            kind: 'group without a public title',
            element: generateElementWithDefaultValues(ElementType.GroupLayout),
            publicField: undefined,
            publicValue: undefined,
        },
    ])('keeps optional naming in the final section for a $kind', ({element, publicField, publicValue}) => {
        const initialElement = {...element, name: 'Vorhandene Bezeichnung'};
        const onChange = vi.fn();
        render(<TestProperties element={initialElement} onChange={onChange}/>);

        const heading = screen.getByRole('heading', {name: 'Weitere Angaben zum Element'});
        const editorName = screen.getByRole('textbox', {name: /^Bezeichnung in der Formularstruktur/});
        const elementId = screen.getByRole('textbox', {name: /^ID des Elements/});

        expect(heading.compareDocumentPosition(editorName) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(editorName.compareDocumentPosition(elementId) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(editorName).toHaveValue(initialElement.name);
        expect(editorName).not.toBeRequired();
        expect(editorName).toHaveAccessibleDescription(/Eine interne Bezeichnung, z\. B\. eine Abkürzung, kann helfen, das Element in der Formularstruktur leichter wiederzufinden/);
        expect(elementId).toHaveAccessibleDescription('Technische Kennung zur eindeutigen Identifikation des Elements. Sie wird automatisch vergeben und ist vor allem für Entwickler:innen relevant.');
        expect(screen.queryByRole('textbox', {name: /^Interner Name/})).not.toBeInTheDocument();

        if (publicField != null) {
            expect(screen.getByRole('textbox', {name: publicField})
                .compareDocumentPosition(heading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        }

        fireEvent.change(editorName, {target: {value: 'Kurzbezeichnung'}});
        expect(onChange).toHaveBeenLastCalledWith({...initialElement, name: 'Kurzbezeichnung'});

        fireEvent.change(editorName, {target: {value: ''}});
        expect(onChange).toHaveBeenLastCalledWith({...initialElement, name: ''});
        expect(editorName).toHaveValue('');
        expect(elementId).toHaveValue(initialElement.id);
        expect(elementId).toBeDisabled();

        if (publicField != null) {
            expect(screen.getByRole('textbox', {name: publicField})).toHaveValue(publicValue);
        }
    });

    it('places the field title before its width control', () => {
        render(<TestProperties element={generateElementWithDefaultValues(ElementType.Text)}/>);

        const title = screen.getByRole('textbox', {name: /^Titel – optional$/});
        const width = screen.getByRole('combobox', {name: 'Breite des Elements in der Darstellung'});
        expect(title.compareDocumentPosition(width) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('does not show an empty basic-properties section inside a summary layout', () => {
        render(<TestProperties
            element={generateElementWithDefaultValues(ElementType.GroupLayout)}
            parents={[generateElementWithDefaultValues(ElementType.SummaryLayout)]}
        />);

        expect(screen.queryByRole('heading', {name: 'Grundlegende Angaben'})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: /^Bezeichnung in der Formularstruktur/})).toBeEnabled();
    });

    it('keeps the editor designation hidden on the root element', () => {
        render(<TestProperties element={generateElementWithDefaultValues(ElementType.FormLayout)} isRoot/>);

        expect(screen.queryByRole('textbox', {name: /^Bezeichnung in der Formularstruktur/})).not.toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: /^ID des Elements/})).toBeInTheDocument();
    });

    it('keeps both fields disabled in read-only mode', () => {
        render(<TestProperties
            element={generateElementWithDefaultValues(ElementType.GroupLayout)}
            editable={false}
            allowElementIdEditing
        />);

        expect(screen.getByRole('textbox', {name: /^Bezeichnung in der Formularstruktur/})).toBeDisabled();
        expect(screen.getByRole('textbox', {name: /^ID des Elements/})).toBeDisabled();
    });

    it.each([
        {allowElementIdEditing: true, displayContext: ElementDisplayContext.CustomerFacing},
        {allowElementIdEditing: false, displayContext: ElementDisplayContext.DataObjectSchema},
    ])('preserves ID editing for $displayContext with the editing flag $allowElementIdEditing', (options) => {
        const element = generateElementWithDefaultValues(ElementType.GroupLayout);
        const onChange = vi.fn();
        render(<TestProperties element={element} onChange={onChange} {...options}/>);

        const elementId = screen.getByRole('textbox', {name: /^ID des Elements/});
        expect(elementId).toBeEnabled();
        fireEvent.change(elementId, {target: {value: 'newId'}});
        expect(onChange).toHaveBeenLastCalledWith({...element, id: 'newId'});
    });
});

function TestProperties({
    element,
    onChange = () => undefined,
    editable = true,
    allowElementIdEditing = false,
    displayContext = ElementDisplayContext.CustomerFacing,
    isRoot = false,
    parents,
}: {
    element: AnyElement;
    onChange?: (element: AnyElement) => void;
    editable?: boolean;
    allowElementIdEditing?: boolean;
    displayContext?: ElementDisplayContext;
    isRoot?: boolean;
    parents?: AnyElement[];
}) {
    const [currentElement, setCurrentElement] = useState(element);
    const [form] = useState<AnyElement>(() => generateElementWithDefaultValues(ElementType.FormLayout));
    const root = isRoot ? currentElement : form;
    const elementParents = parents ?? (isRoot ? [] : [root]);
    const treeContext: ElementTreeContextType = {
        root,
        editable,
        allElements: [{element: currentElement, parents: elementParents}],
        allowElementIdEditing,
        displayContext,
        scrollToElement: () => undefined,
        canDropElement: () => false,
        moveElement: () => undefined,
        expandCommand: {type: null, version: 0},
        initiallyExpandedSectionIds: new Set(),
    };

    return (
        <ElementTreeContextProvider value={treeContext}>
            <ElementTreeEditorContextProvider value={{
                currentElement,
                parents: elementParents,
                onChangeCurrentElement: (updatedElement) => {
                    setCurrentElement(updatedElement);
                    onChange(updatedElement);
                },
            }}>
                <ElementTreeEditorContentTabProperties/>
            </ElementTreeEditorContextProvider>
        </ElementTreeContextProvider>
    );
}

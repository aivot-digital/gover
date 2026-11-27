import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ReferenceCheck} from './reference-check';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyElement} from '../../../../models/elements/any-element';
import {ElementWithParents} from '../../../../utils/flatten-elements';

const firstName = {id: 'tx_first_name', type: ElementType.Text, label: 'Vorname'} as AnyElement;
const age = {id: 'nm_age', type: ElementType.Number, label: 'Alter'} as AnyElement;
const current = {id: 'tx_current', type: ElementType.Text, label: 'Aktuelles Feld'} as AnyElement;
const allElements: ElementWithParents[] = [firstName, age, current].map((element) => ({element, parents: []}));

function renderReferenceCheck(code: string) {
    render(
        <ReferenceCheck
            allElements={allElements}
            element={current}
            lowCodeOld={[]}
            lowCode={[code]}
            noCodeOld={[]}
            noCode={[]}
        />,
    );
}

describe('ReferenceCheck', () => {
    it('lists elements referenced through ctx.effectiveValues and ctx.elementStates', () => {
        renderReferenceCheck('return ctx.effectiveValues.tx_first_name === "Ada" && ctx.elementStates.nm_age.visible;');

        expect(screen.getByText('tx_first_name')).toBeInTheDocument();
        expect(screen.getByText('nm_age')).toBeInTheDocument();
    });

    it('lists explicitly marked references', () => {
        renderReferenceCheck('//>>>nm_age\nreturn ctx.effectiveValues["nm_" + "age"] != null;');

        expect(screen.getByText('nm_age')).toBeInTheDocument();
        expect(screen.queryByText('tx_first_name')).not.toBeInTheDocument();
    });

    it('does not treat the context properties as element references', () => {
        renderReferenceCheck('return Object.keys(ctx.effectiveValues).length > 0;');

        expect(screen.getByText('Keine referenzierten Elemente gefunden.')).toBeInTheDocument();
    });
});

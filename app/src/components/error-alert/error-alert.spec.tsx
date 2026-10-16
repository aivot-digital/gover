import {render, screen, within} from '@testing-library/react';
import {describe, expect, it} from 'vitest';
import {ErrorAlert} from './error-alert';
import {ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';
import {createDerivedRuntimeElementData} from '../../models/element-data';

describe('ErrorAlert', () => {
    it.each([ElementType.PaymentConfigElement, ElementType.IdentityConfigElement])('lists individual configuration errors for %s', (type) => {
        render(<ErrorAlert
            element={{id: 'configuration', type, label: 'Konfiguration'} as AnyElement}
            authoredElementValues={{}}
            derivedData={createDerivedRuntimeElementData({elementStates: {
                configuration: {error: 'Konfiguration fehlerhaft.', errorDetails: {messages: ['Beschreibung fehlt.', 'Position 2: Betrag fehlt.']}},
            }})}
        />);

        const errors = within(screen.getByRole('alert')).getAllByRole('listitem');
        expect(errors).toHaveLength(2);
        expect(errors[0]).toHaveTextContent('Beschreibung fehlt.');
        expect(errors[1]).toHaveTextContent('Position 2: Betrag fehlt.');
        expect(screen.queryByText('Konfiguration fehlerhaft.')).not.toBeInTheDocument();
    });
});

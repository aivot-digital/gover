import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {type NoCodeOperatorDetailsDTO} from '../../models/dtos/no-code-operator-details-dto';
import {OperatorInfo} from './operator-info';

const operator: NoCodeOperatorDetailsDTO = {
    identifier: 'equals',
    packageName: 'core',
    label: 'Ist gleich',
    description: '# Beschreibung\n\nVergleicht **zwei Werte**.\n\n## Beispiel\n\n`a = b`',
    abstractDescription: 'Vergleicht zwei Werte.',
    humanReadableTemplate: null,
    tags: [],
    signatures: [{
        returnType: NoCodeDataType.Boolean,
        parameters: [{label: 'Wert', type: NoCodeDataType.String, description: null, options: []}],
    }, {
        returnType: NoCodeDataType.Boolean,
        parameters: [],
    }],
};

describe('OperatorInfo', () => {
    it('keeps description formatting and subordinate headings while technical data starts collapsed', () => {
        render(<OperatorInfo operator={operator} headingBaseLevel={4}/>);

        expect(screen.getByRole('heading', {name: 'Beschreibung', level: 4})).toBeVisible();
        expect(screen.getByRole('heading', {name: 'Beispiel', level: 5})).toBeVisible();
        expect(screen.getByRole('heading', {name: 'Technische Informationen', level: 4})).toBeVisible();
        expect(screen.getByText('zwei Werte').tagName).toBe('STRONG');
        expect(screen.getByText('a = b').tagName).toBe('CODE');
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('reveals the complete package, identifier and signatures from the shared technical section', async () => {
        const user = userEvent.setup();
        const longIdentifier = 'function-identifier-'.repeat(20);
        const longPackage = 'package-name-'.repeat(20);
        render(<OperatorInfo operator={{...operator, identifier: longIdentifier, packageName: longPackage}}/>);

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        const details = screen.getByRole('region', {name: 'Technische Informationen'});
        expect(within(details).getAllByRole('rowheader').map((cell) => cell.textContent))
            .toEqual(['Paket', 'Eindeutige ID', 'Signaturen']);
        expect(within(details).getByRole('cell', {name: longIdentifier})).toHaveStyle({overflowWrap: 'anywhere'});
        expect(within(details).getByRole('cell', {name: longPackage})).toHaveStyle({overflowWrap: 'anywhere'});
        expect(within(details).getAllByRole('listitem').map((item) => item.textContent?.replace(/\s+/g, ' ').trim()))
            .toEqual(['Wert: Textwert -> Wahrheitswert', 'Keine Parameter -> Wahrheitswert']);
    });

    it('omits the package row for functions without a package', async () => {
        const user = userEvent.setup();
        render(<OperatorInfo operator={{...operator, packageName: ''}}/>);

        await user.click(screen.getByRole('button', {name: 'Technische Informationen'}));

        expect(screen.queryByRole('rowheader', {name: 'Paket'})).not.toBeInTheDocument();
        expect(screen.getByRole('rowheader', {name: 'Eindeutige ID'})).toBeVisible();
    });
});

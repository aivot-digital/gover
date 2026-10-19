import {describe, expect, it} from 'vitest';
import {type IdentityData, type IdentityDataMap} from '../../identity/models/identity-data';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../enums/process-task-status';
import {getProcessTaskStatusDisplay} from './process-task-status-label';

const applicant: IdentityData = {
    identityId: 'applicant', title: ' Antragstellende ', type: 'Email',
    emailAddress: 'person@example.org', attributes: {}, providerKey: null, metadataIdentifier: null,
    uniqueIdFromIdentityProvider: null, communicationProviderBindingId: null, communicationProviderData: {},
};
const identities: IdentityDataMap = {
    other: {...applicant, identityId: 'other', title: 'Andere Beteiligte'},
    storedKey: applicant,
};
const waiting = {status: ProcessTaskStatus.AwaitingCustomer, assignedCustomerIdentityId: 'applicant'};

describe('task status display', () => {
    it('resolves the assigned identity among multiple entries by its identity ID', () => {
        expect(getProcessTaskStatusDisplay(waiting, identities)).toEqual({
            identityLabel: 'Antragstellende',
            systemLabel: 'Wartet auf Antragstellende',
            label: 'Wartet auf Antragstellende',
            tooltip: 'Wartet auf Antragstellende',
        });
    });

    it('prefers the resolved label supplied by the list API', () => {
        const display = getProcessTaskStatusDisplay({...waiting, assignedCustomerIdentityLabel: ' Vertretung '}, identities);
        expect(display.label).toBe('Wartet auf Vertretung');
        expect(display.identityLabel).toBe('Vertretung');
    });

    it.each([null, undefined, '', '   '])('resolves the identity when the supplied label is %s', assignedCustomerIdentityLabel => {
        expect(getProcessTaskStatusDisplay({...waiting, assignedCustomerIdentityLabel}, identities).label)
            .toBe('Wartet auf Antragstellende');
    });

    it.each([null, undefined, '', '   '])('uses the identity ID when its title is %s', title => {
        expect(getProcessTaskStatusDisplay(waiting, {applicant: {...applicant, title}}).label)
            .toBe('Wartet auf applicant');
    });

    it.each<[IdentityDataMap | null | undefined]>([[null], [undefined], [{}], [{other: identities.other}]])('uses the assigned ID when its identity is unavailable (%s)', data => {
        expect(getProcessTaskStatusDisplay(waiting, data).label).toBe('Wartet auf applicant');
    });

    it.each([null, undefined, '', '   '])('keeps the generic status without an assigned identity (%s)', assignedCustomerIdentityId => {
        expect(getProcessTaskStatusDisplay({...waiting, assignedCustomerIdentityId}, identities)).toEqual({
            identityLabel: null, label: 'Wartet auf Rückmeldung', systemLabel: 'Wartet auf Rückmeldung', tooltip: undefined,
        });
    });

    it('prioritizes the custom status and explains the identity-based system status', () => {
        expect(getProcessTaskStatusDisplay({...waiting, statusOverride: ' Unterlagen angefordert '}, identities)).toEqual({
            identityLabel: 'Antragstellende', label: 'Unterlagen angefordert',
            systemLabel: 'Wartet auf Antragstellende',
            tooltip: 'Unterlagen angefordert (Systemstatus: Wartet auf Antragstellende)',
        });
    });

    it.each([null, undefined, '', '   ', ' Wartet auf Antragstellende '])('avoids repeating the system status for override %s', statusOverride => {
        const display = getProcessTaskStatusDisplay({...waiting, statusOverride}, identities);
        expect(display.label).toBe('Wartet auf Antragstellende');
        expect(display.tooltip).toBe('Wartet auf Antragstellende');
    });

    it.each(Object.values(ProcessTaskStatus).filter(status => status !== ProcessTaskStatus.AwaitingCustomer))(
        'preserves the generic label for %s despite an assigned identity', status => {
            expect(getProcessTaskStatusDisplay({...waiting, status, assignedCustomerIdentityLabel: 'Antragstellende'}, identities))
                .toEqual({label: ProcessTaskStatusLabels[status], systemLabel: ProcessTaskStatusLabels[status], identityLabel: null, tooltip: undefined});
        },
    );
});

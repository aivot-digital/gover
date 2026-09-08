import type {AuthoredElementValues} from '../../../models/element-data';
import type {IdentityCommunicationState, IdentitySlot} from './identity-slot';

/**
 * Operations needed by the shared customer identity selection controls.
 *
 * Form and customer-task pages provide context-specific implementations so the
 * controls do not need to know which public endpoint owns an identity slot.
 */
export interface IdentitySelectionApi {
    createIdentityProviderStartLink: (
        identityId: string,
        providerKey: string,
        origin: string,
    ) => string;
    setEmailIdentity: (identityId: string, emailAddress: string) => Promise<IdentitySlot>;
    clearIdentity: (identityId: string) => Promise<void>;
    selectCommunication: (
        identityId: string,
        bindingId: number,
        customerData: AuthoredElementValues,
    ) => Promise<IdentityCommunicationState>;
    deriveCommunication: (
        identityId: string,
        bindingId: number,
        customerData: AuthoredElementValues,
    ) => Promise<IdentityCommunicationState>;
}

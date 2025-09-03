import {RootElement} from '../../../models/elements/root-element';
import {IdentityProviderLink} from '../../identity/models/identity-provider-link';

export interface FormCitizenDetailsResponseDTO {
    id: number;
    slug: string;
    version: number;
    title: string;
    root: RootElement;
    legalSupportDepartmentId?: number;
    technicalSupportDepartmentId?: number;
    imprintDepartmentId?: number;
    privacyDepartmentId?: number;
    accessibilityDepartmentId?: number;
    developingDepartmentId: number;
    managingDepartmentId?: number;
    responsibleDepartmentId?: number;
    themeId: number;
    identityRequired: boolean;
    identityProviders: IdentityProviderLink[];
}
import type {AuthoredElementValues} from '../../../models/element-data';
import type {GroupLayout} from '../../../models/elements/form/layout/group-layout';
import type {IdentityProviderOption, IdentitySlot} from '../../identity/models/identity-slot';
import type {TaskViewEvent} from '../services/process-instance-task-api-service';

export interface CustomerTaskExistingIdentitySlot {
    id: string;
    isReady: boolean;
    identityProvider: IdentityProviderOption;
}

interface CustomerTaskIdentityRequirements {
    newIdentitySlot: IdentitySlot | null;
    existingIdentitySlot: CustomerTaskExistingIdentitySlot | null;
}

export interface ReadyCustomerTaskView extends CustomerTaskIdentityRequirements {
    layout: GroupLayout;
    data: AuthoredElementValues;
    events: TaskViewEvent[];
}

export interface BlockedCustomerTaskView extends CustomerTaskIdentityRequirements {
    layout: null;
    data: null;
    events: null;
}

export type CustomerTaskViewResponse = ReadyCustomerTaskView | BlockedCustomerTaskView;

export function hasCustomerTaskViewContent(view: CustomerTaskViewResponse): view is ReadyCustomerTaskView {
    return view.layout != null && view.data != null && view.events != null;
}

export function hasCustomerTaskIdentityRequirements(view: CustomerTaskViewResponse): boolean {
    return view.newIdentitySlot != null || view.existingIdentitySlot != null;
}

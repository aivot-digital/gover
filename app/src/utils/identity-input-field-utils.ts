import {AnyElement} from '../models/elements/any-element';
import {ElementType} from '../data/element-type/element-type';
import {AuthoredElementValues} from '../models/element-data';
import {
    IdentityInputFieldElement,
    IdentityInputFieldElementItem,
    IdentityInputFieldOption,
} from '../models/elements/form/input/identity-input-field-element';

export const IdentityInputMailAttributeKey = 'email';
export const PendingIdentityInputAuthContextSessionKey = 'identity-input-auth-context';

export interface PendingIdentityInputAuthContext {
    elementId: string;
    stepId: string | null;
    stepIndex: number;
    optionIdentityProviderKey: string | null;
    returnUrl: string;
    authoredElementValues: AuthoredElementValues;
}

export function createIdentityInputMailValue(email: string | null | undefined): IdentityInputFieldElementItem | undefined {
    if (email == null || email.trim().length === 0) {
        return undefined;
    }

    return {
        identityProviderKey: null,
        identityAttributes: {
            [IdentityInputMailAttributeKey]: email.trim(),
        },
    };
}

export function extractIdentityInputMailValue(
    value: IdentityInputFieldElementItem | null | undefined,
): string | undefined {
    if (value?.identityProviderKey != null) {
        return undefined;
    }

    const rawMail = value?.identityAttributes?.[IdentityInputMailAttributeKey];
    return typeof rawMail === 'string' && rawMail.trim().length > 0 ? rawMail.trim() : undefined;
}

export function getIdentityInputOptionForProvider(
    element: IdentityInputFieldElement,
    providerKey: string | null | undefined,
): IdentityInputFieldOption | undefined {
    if (providerKey == null) {
        return undefined;
    }

    return (element.options ?? [])
        .find((option) => option.identityProviderKey === providerKey);
}

export function isElementNestedInReplicatingContainer(parents: AnyElement[]): boolean {
    return parents.some((parent) => parent.type === ElementType.ReplicatingContainer);
}

export function storePendingIdentityInputAuthContext(context: PendingIdentityInputAuthContext): void {
    sessionStorage.setItem(PendingIdentityInputAuthContextSessionKey, JSON.stringify(context));
}

export function loadPendingIdentityInputAuthContext(): PendingIdentityInputAuthContext | null {
    const raw = sessionStorage.getItem(PendingIdentityInputAuthContextSessionKey);
    if (raw == null) {
        return null;
    }

    try {
        const parsed = JSON.parse(raw) as Partial<PendingIdentityInputAuthContext>;
        if (parsed.elementId == null || parsed.returnUrl == null) {
            return null;
        }

        return {
            elementId: parsed.elementId,
            stepId: parsed.stepId ?? null,
            stepIndex: parsed.stepIndex ?? 0,
            optionIdentityProviderKey: parsed.optionIdentityProviderKey ?? null,
            returnUrl: parsed.returnUrl,
            authoredElementValues: parsed.authoredElementValues ?? {},
        };
    } catch (error) {
        console.error('Unable to parse pending identity input auth context:', error);
        return null;
    }
}

export function clearPendingIdentityInputAuthContext(): void {
    sessionStorage.removeItem(PendingIdentityInputAuthContextSessionKey);
}

export function clampStepIndex(stepIndex: number, stepCount: number): number {
    if (stepCount <= 0) {
        return 0;
    }

    return Math.min(Math.max(stepIndex, 0), stepCount - 1);
}

import {StorageScope, StorageService} from './storage-service';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {AppInfo} from '../app-info';
import {AuthoredElementValues, hasAuthoredElementValuesSomeInput} from '../models/element-data';
import {cleanAuthoredElementValues} from '../utils/element-data-utils';
import {FormLayoutElement} from '../models/elements/form-layout-element';
import {InstantIso} from '../utils/temporal-types';
import {isInstantIso} from '../utils/temporal-utils';

const MAJOR_VERSION = AppInfo.version.split('.')[0];
const DATA_KEY = 'state';
const DATE_KEY = 'date';

export interface CustomerInputDraft {
    date: InstantIso;
    data: AuthoredElementValues;
}

export class CustomerInputService {
    public static loadCustomerInputDraft(processSlug: string, formSlug: string, version: number): CustomerInputDraft | null {
        const date = this.loadCustomerInputDate(processSlug, formSlug, version);
        const data = this.loadCustomerInputState(processSlug, formSlug, version);

        if (date != null && data != null && hasAuthoredElementValuesSomeInput(data)) {
            return {
                date,
                data,
            };
        }

        return null;
    }

    public static loadCustomerInputDate(processSlug: string, formSlug: string, version: number): InstantIso | null {
        const rawDate = this
            .getKeys(processSlug, formSlug, version, DATE_KEY)
            .map((key) => StorageService.loadString_unsafe(key))
            .find((value) => value != null);

        return isInstantIso(rawDate) ? rawDate : null;
    }

    public static loadCustomerInputState(processSlug: string, formSlug: string, version: number): AuthoredElementValues | null {
        for (const key of this.getKeys(processSlug, formSlug, version, DATA_KEY)) {
            const state = StorageService.loadObject_unsafe<AuthoredElementValues>(key);
            if (state != null) {
                return state;
            }
        }

        return null;
    }

    public static storeCustomerInput(processSlug: string, formSlug: string, version: number, root: FormLayoutElement, state: AuthoredElementValues): void {
        const stateCopy = cleanAuthoredElementValues(root, state);
        delete stateCopy[IdentityCustomerInputKey];

        if (!hasAuthoredElementValuesSomeInput(stateCopy)) {
            this.cleanCustomerInput(processSlug, formSlug, version);
            return;
        }

        StorageService.storeObject_unsafe(this.getKey(processSlug, formSlug, version, DATA_KEY), stateCopy, StorageScope.Local);
        StorageService.storeString_unsafe(this.getKey(processSlug, formSlug, version, DATE_KEY), new Date().toISOString(), StorageScope.Local);
    }

    public static cleanCustomerInput(processSlug: string, formSlug: string, version: number): void {
        for (const key of this.getKeys(processSlug, formSlug, version, DATA_KEY)) {
            StorageService.clearItem_unsafe(key);
        }
        for (const key of this.getKeys(processSlug, formSlug, version, DATE_KEY)) {
            StorageService.clearItem_unsafe(key);
        }
    }

    private static getKeys(processSlug: string, formSlug: string, version: number, suffix: string): string[] {
        const keys = [
            this.getKey(processSlug, formSlug, version, suffix),
            this.getLegacyKey(formSlug, version, suffix),
        ];

        return Array.from(new Set(keys));
    }

    private static getKey(processSlug: string, formSlug: string, version: number, suffix: string): string {
        return `${processSlug}-${formSlug}-${version}-${MAJOR_VERSION}-${suffix}`;
    }

    private static getLegacyKey(formSlug: string, version: number, suffix: string): string {
        return `${formSlug}-${version}-${MAJOR_VERSION}-${suffix}`;
    }
}

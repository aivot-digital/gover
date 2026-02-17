import {StorageScope, StorageService} from './storage-service';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {AppInfo} from '../app-info';
import {ElementData} from '../models/element-data';
import {cleanElementData} from '../utils/element-data-utils';
import {RootElement} from '../models/elements/root-element';

const MAJOR_VERSION = AppInfo.version.split('.')[0];
const DATA_KEY = 'state';
const DATE_KEY = 'date';

export class CustomerInputService {
    public static loadCustomerInputDate(slug: string, version: number): Date | null {
        const rawDate = StorageService.loadString_unsafe(this.getKey(slug, version, DATE_KEY));
        if (rawDate != null) {
            try {
                return new Date(rawDate);
            } catch (e) {
                return null;
            }
        }
        return null;
    }

    public static loadCustomerInputState(slug: string, version: number): ElementData | null {
        const key = this.getKey(slug, version, DATA_KEY);
        return StorageService.loadObject_unsafe<ElementData>(key);
    }

    public static storeCustomerInput(slug: string, version: number, root: RootElement, state: ElementData): void {
        const stateCopy = cleanElementData(root, state);
        delete stateCopy[IdentityCustomerInputKey];
        StorageService.storeObject_unsafe(this.getKey(slug, version, DATA_KEY), stateCopy, StorageScope.Local);
        StorageService.storeString_unsafe(this.getKey(slug, version, DATE_KEY), new Date().toISOString(), StorageScope.Local);
    }

    public static cleanCustomerInput(slug: string, version: number): void {
        StorageService.clearItem_unsafe(this.getKey(slug, version, DATA_KEY));
        StorageService.clearItem_unsafe(this.getKey(slug, version, DATE_KEY));
    }

    private static getKey(slug: string, version: number, suffix: string): string {
        return `${slug}-${version}-${MAJOR_VERSION}-${suffix}`;
    }
}

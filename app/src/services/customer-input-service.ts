import {type Form as Application} from '../models/entities/form';
import {StorageScope, StorageService} from './storage-service';
import {IdentityCustomerInputKey} from '../modules/identity/constants/identity-customer-input-key';
import {AppInfo} from '../app-info';
import {ElementData} from '../models/element-data';

const MAJOR_VERSION = AppInfo.version.split('.')[0];
const DATA_KEY = 'state';
const DATE_KEY = 'date';

export class CustomerInputService {
    public static loadCustomerInputDate(application: Application): Date | null {
        const rawDate = StorageService.loadString_unsafe(this.getKey(application, DATE_KEY));
        if (rawDate != null) {
            try {
                return new Date(rawDate);
            } catch (e) {
                return null;
            }
        }
        return null;
    }

    public static loadCustomerInputState(form: Application): ElementData | null {
        const key = this.getKey(form, DATA_KEY);
        return StorageService.loadObject_unsafe<ElementData>(key);
    }

    public static storeCustomerInput(application: Application, state: ElementData): void {
        const stateCopy = {...state};
        delete stateCopy[IdentityCustomerInputKey];
        StorageService.storeObject_unsafe(this.getKey(application, DATA_KEY), stateCopy, StorageScope.Local);
        StorageService.storeString_unsafe(this.getKey(application, DATE_KEY), new Date().toISOString(), StorageScope.Local);
    }

    public static cleanCustomerInput(application: Application): void {
        StorageService.clearItem_unsafe(this.getKey(application, DATA_KEY));
        StorageService.clearItem_unsafe(this.getKey(application, DATE_KEY));
    }

    private static getKey(application: Application, suffix: string): string {
        return `${application.slug}-${application.version}-${MAJOR_VERSION}-${suffix}`;
    }
}

import {CustomerInput} from '../models/customer-input';
import {Application} from '../models/application';

const DATA_KEY = 'state';
const DATE_KEY = 'date';

export class UserInputService {
    static loadUserInputDate(application: Application): Date | null {
        if (this.storageIsAccessible()) {
            const rawDate = localStorage.getItem(this.getKey(application, DATE_KEY));
            if (rawDate != null) {
                try {
                    return new Date(rawDate);
                } catch (e) {
                }
            }
        }
        return null;
    }

    static loadUserInputState(application: Application): CustomerInput | null {
        const key = this.getKey(application, DATA_KEY);
        if (this.storageIsAccessible()) {
            const rawState = localStorage.getItem(key);
            if (rawState != null) {
                try {
                    const parsedState = JSON.parse(rawState);
                    if (typeof parsedState === 'object') {
                        return parsedState as CustomerInput;
                    }
                } catch (e) {
                }
            }
        }
        return null;
    }

    static storeUserInput(application: Application, state: CustomerInput): void {
        if (this.storageIsAccessible()) {
            localStorage.setItem(this.getKey(application, DATA_KEY), JSON.stringify(state));
            localStorage.setItem(this.getKey(application, DATE_KEY), new Date().toISOString());
        }
    }

    static cleanUserInput(application: Application): void {
        localStorage.removeItem(this.getKey(application, DATA_KEY));
        localStorage.removeItem(this.getKey(application, DATE_KEY));
    }

    private static getKey(application: Application, suffix: string): string {
        return `${application.slug}-${application.version}-${suffix}`;
    }

    private static storageIsAccessible(): boolean {
        return typeof localStorage !== 'undefined';
    }
}

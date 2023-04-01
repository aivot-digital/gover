import {LocalstorageKey} from "../data/localstorage-key";

class _LocalStorageService {
    public storeString(key: LocalstorageKey, value: string): void {
        localStorage.setItem(_LocalStorageService.generateKey(key), value);
    }

    public loadString(key: LocalstorageKey): string | null {
        return localStorage.getItem(_LocalStorageService.generateKey(key));
    }

    public storeObject(key: LocalstorageKey, value: any): void {
        this.storeString(key, JSON.stringify(value));
    }

    public loadObject<T>(key: LocalstorageKey): T | null {
        const str = this.loadString(key);
        if (str == null) {
            return null;
        }
        try {
            return JSON.parse(str);
        } catch (error) {
            console.error(error);
            return null;
        }
    }

    public hasStored(key: LocalstorageKey): boolean {
        return localStorage.getItem(_LocalStorageService.generateKey(key)) != null;
    }

    public clearItem(key: LocalstorageKey): void {
        localStorage.removeItem(_LocalStorageService.generateKey(key));
    }

    private static generateKey(key: LocalstorageKey) {
        return `gover-${key}`;
    }
}

export const LocalStorageService = new _LocalStorageService();

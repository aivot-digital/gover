import {type StorageKey} from '../data/storage-key';

export enum StorageScope {
    Local,
    Session,
}

class _StorageService {
    public storeString(key: StorageKey, value: string, scope: StorageScope): void {
        this.storeString_unsafe(key, value, scope);
    }

    public storeString_unsafe(key: string, value: string, scope: StorageScope): void {
        const _key = this.generateKey(key);
        switch (scope) {
            case StorageScope.Local:
                localStorage.setItem(_key, value);
                break;
            case StorageScope.Session:
                sessionStorage.setItem(_key, value);
                break;
        }
    }

    public loadString(key: StorageKey): string | null {
        return this.loadString_unsafe(key);
    }

    public loadString_unsafe(key: string): string | null {
        const _key = this.generateKey(key);

        const sessionResult = sessionStorage.getItem(_key);
        if (sessionResult != null) {
            return sessionResult;
        }
        return localStorage.getItem(_key);
    }

    public storeFlag(key: StorageKey, value: boolean, scope: StorageScope): void {
        if (value) {
            switch (scope) {
                case StorageScope.Local:
                    localStorage.setItem(this.generateKey(key), 'yes');
                    break;
                case StorageScope.Session:
                    sessionStorage.setItem(this.generateKey(key), 'yes');
                    break;
            }
        } else {
            this.clearItem(key);
        }
    }

    public loadFlag(key: StorageKey): boolean {
        const _key = this.generateKey(key);
        return sessionStorage.getItem(_key) != null || localStorage.getItem(_key) != null;
    }

    public storeObject(key: StorageKey, value: any, scope: StorageScope): void {
        this.storeObject_unsafe(key, value, scope);
    }

    public storeObject_unsafe(key: string, value: any, scope: StorageScope): void {
        this.storeString_unsafe(key, JSON.stringify(value), scope);
    }

    public loadObject<T>(key: StorageKey): T | null {
        return this.loadObject_unsafe<T>(key);
    }

    public loadObject_unsafe<T>(key: string): T | null {
        const str = this.loadString_unsafe(key);
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

    public clearItem(key: StorageKey): void {
        this.clearItem_unsafe(key);
    }

    public clearItem_unsafe(key: string): void {
        const _key = this.generateKey(key);
        sessionStorage.removeItem(_key);
        localStorage.removeItem(_key);
    }

    public hasItem(key: StorageKey): boolean {
        return this.hasItem_unsafe(key);
    }

    public hasItem_unsafe(key: string): boolean {
        const _key = this.generateKey(key);
        return sessionStorage.getItem(_key) != null || localStorage.getItem(_key) != null;
    }

    public generateKey(key: string): string {
        return `gover-${key}`;
    }
}

export const StorageService = new _StorageService();

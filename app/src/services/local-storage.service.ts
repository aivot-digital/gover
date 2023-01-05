class _LocalStorageService {
    public storeString(key: string, value: string): void {
        localStorage.setItem(_LocalStorageService.generateKey(key), value);
    }

    public loadString(key: string): string | null {
        return localStorage.getItem(_LocalStorageService.generateKey(key));
    }

    public storeObject(key: string, value: any): void {
        this.storeString(key, JSON.stringify(value));
    }

    public loadObject<T>(key: string): T | null {
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

    public hasStored(key: string): boolean {
        return localStorage.getItem(_LocalStorageService.generateKey(key)) != null;
    }

    public clearItem(key: string): void {
        localStorage.removeItem(_LocalStorageService.generateKey(key));
    }

    private static generateKey(key: string) {
        return `gover-${key}`;
    }
}

export const LocalStorageService = new _LocalStorageService();

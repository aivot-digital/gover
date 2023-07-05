import {ApiService} from "./api-service";


class _AssetService {

    public async list(): Promise<string[]> {
        return ApiService.get('system-assets');
    }

    public async create(form: FormData): Promise<void> {
        return ApiService.postFormData('system-assets', form);
    }

    public async destroy(name: string): Promise<void> {
        return ApiService.delete(`system-assets/${name}`);
    }

    public getLink(name: string): string {
        return `/api/public/system-assets/${name}`;
    }
}

export const AssetService = new _AssetService();

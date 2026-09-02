import {BaseApiService} from '../../services/base-api-service';

export class JavascriptApiService extends BaseApiService {
    public async getTypes(): Promise<string> {
        const blob = await this.getBlob('/api/javascript-function-providers/types.d.ts');
        return await blob.text();
    }
}
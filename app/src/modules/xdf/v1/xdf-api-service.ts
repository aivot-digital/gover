import {BaseApiService} from '../../../services/base-api-service';
import {FormLayoutElement} from '../../../models/elements/form-layout-element';

export class XdfApiService extends BaseApiService {
    public xdfTransform(value: string | ArrayBuffer): Promise<FormLayoutElement> {
        return this.postXml<string | ArrayBuffer, FormLayoutElement>('/api/xdf/v2/transform/', value);
    }
}
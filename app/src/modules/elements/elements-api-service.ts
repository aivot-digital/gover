import {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';
import {BaseApiService} from '../../services/base-api-service';

interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

interface ElementDerivationRequest {
    element: AnyElement;
    authoredElementValues: AuthoredElementValues;
    derivationOptions: ElementDerivationOptions;
}

export class ElementsApiService extends BaseApiService {
    public async derive(request: ElementDerivationRequest): Promise<DerivedRuntimeElementData> {
        return await this.post<ElementDerivationRequest, DerivedRuntimeElementData>('/api/elements/derive/', request);
    }
}

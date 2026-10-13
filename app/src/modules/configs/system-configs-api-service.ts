import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {SystemConfigRequestDto} from './dtos/system-config-request-dto';
import {SystemConfigResponseDto} from './dtos/system-config-response-dto';
import {SystemConfigDefinitionResponseDTO} from './dtos/system-config-definition-response-dto';
import {type AuthoredElementValues, type DerivedRuntimeElementData} from '../../models/element-data';
import {type ElementDerivationOptions} from '../elements/elements-api-service';

interface SystemConfigsFilter {
    publicConfig: boolean;
}

export class SystemConfigsApiService extends CrudApiService<SystemConfigRequestDto, SystemConfigResponseDto, SystemConfigResponseDto, SystemConfigResponseDto, SystemConfigResponseDto, string, SystemConfigsFilter> {
    public constructor(api: Api) {
        super(api, 'system-configs/');
    }

    public initialize(): SystemConfigResponseDto {
        return {
            key: '',
            value: '',
            publicConfig: false,
        };
    }

    public async listDefinitions(): Promise<SystemConfigDefinitionResponseDTO[]> {
        return await this.api.get<SystemConfigDefinitionResponseDTO[]>(`system-configs/definitions/`);
    }

    public async deriveDefinitionCategory(category: string,
                                          authoredElementValues: AuthoredElementValues,
                                          derivationOptions: ElementDerivationOptions): Promise<DerivedRuntimeElementData> {
        return await this.api.post<DerivedRuntimeElementData>('system-configs/definitions/derive/', {
            authoredElementValues,
            derivationOptions,
        }, {
            queryParams: {category},
        });
    }
}
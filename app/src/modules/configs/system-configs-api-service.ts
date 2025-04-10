import {CrudApiService} from '../../services/crud-api-service';
import {Api} from '../../hooks/use-api';
import {SystemConfigRequestDto} from './dtos/system-config-request-dto';
import {SystemConfigResponseDto} from './dtos/system-config-response-dto';
import {SystemConfigDefinitionResponseDTO} from './dtos/system-config-definition-response-dto';

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
        return await this.api.get<SystemConfigDefinitionResponseDTO[]>(`system-config-definitions/`);
    }
}
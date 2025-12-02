import {BaseCrudApiService} from '../../services/base-crud-api-service';
import {ResourceAccessControlRequestDto} from './dtos/resource-access-control-request-dto';
import {ResourceAccessControlResponseDto} from './dtos/resource-access-control-response-dto';

export interface ResourceAccessControlFilter {
    id?: number;
    ids?: number[];
    sourceTeamId?: number;
    sourceTeamIds?: number[];
    sourceDepartmentId?: number;
    sourceDepartmentIds?: number[];
    targetFormId?: number;
    targetFormIds?: number[];
    targetProcessId?: number;
    targetProcessIds?: number[];
    targetProcessInstanceId?: number;
    targetProcessInstanceIds?: number[];
    formPermissionCreate?: boolean;
    formPermissionRead?: boolean;
    formPermissionEdit?: boolean;
    formPermissionDelete?: boolean;
    formPermissionAnnotate?: boolean;
    formPermissionPublish?: boolean;
    processPermissionCreate?: boolean;
    processPermissionRead?: boolean;
    processPermissionEdit?: boolean;
    processPermissionDelete?: boolean;
    processPermissionAnnotate?: boolean;
    processPermissionPublish?: boolean;
    processInstancePermissionCreate?: boolean;
    processInstancePermissionRead?: boolean;
    processInstancePermissionEdit?: boolean;
    processInstancePermissionDelete?: boolean;
    processInstancePermissionAnnotate?: boolean;
    created?: string;
    updated?: string;
}

export class ResourceAccessControlsApiService extends BaseCrudApiService<
    ResourceAccessControlRequestDto,
    ResourceAccessControlResponseDto,
    ResourceAccessControlResponseDto,
    ResourceAccessControlRequestDto,
    number,
    ResourceAccessControlFilter
> {
    constructor() {
        super('/api/resource-access-controls/');
    }

    public initialize(): ResourceAccessControlResponseDto {
        return ResourceAccessControlsApiService.initialize();
    }

    public static initialize(): ResourceAccessControlResponseDto {
        return {
            id: 0,
            sourceTeamId: null,
            sourceOrgUnitId: null,
            targetFormId: null,
            targetProcessId: null,
            targetProcessInstanceId: null,
            formPermissionCreate: false,
            formPermissionRead: false,
            formPermissionEdit: false,
            formPermissionDelete: false,
            formPermissionAnnotate: false,
            formPermissionPublish: false,
            processPermissionCreate: false,
            processPermissionRead: false,
            processPermissionEdit: false,
            processPermissionDelete: false,
            processPermissionAnnotate: false,
            processPermissionPublish: false,
            processInstancePermissionCreate: false,
            processInstancePermissionRead: false,
            processInstancePermissionEdit: false,
            processInstancePermissionDelete: false,
            processInstancePermissionAnnotate: false,
            created: '',
            updated: '',
        };
    }
}
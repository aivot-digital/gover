import {ProcessNodeEntity} from "../../../../../entities/process-node-entity";
import {ProcessNodeProvider} from "../../../../../services/process-node-provider-api-service";
import {isStringNotNullOrEmpty} from "../../../../../../../utils/string-utils";

export function getNodeName(node: ProcessNodeEntity, provider: ProcessNodeProvider): string {
    if (isStringNotNullOrEmpty(node.name)) {
        return node.name!;
    }
    return provider.name;
}

export function getNodeDescription(node: ProcessNodeEntity, provider: ProcessNodeProvider): string {
    if (isStringNotNullOrEmpty(node.description)) {
        return node.description!;
    }
    return provider.description;
}
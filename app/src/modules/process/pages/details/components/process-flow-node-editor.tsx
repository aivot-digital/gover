import {ProcessDefinitionNodeEntity} from "../../../entities/process-definition-node-entity";
import {useContext, useEffect, useState} from "react";
import {GroupLayout} from "../../../../../models/elements/form/layout/group-layout";
import {ProcessDefinitionNodeApiService} from "../../../services/process-definition-node-api-service";
import {Box, Button, Skeleton} from "@mui/material";
import {TextFieldComponent} from "../../../../../components/text-field/text-field-component";
import {ElementDerivationContext} from "../../../../elements/components/element-derivation-context";
import {useParams} from "react-router-dom";
import {ProcessDetailsPageContext} from "../process-details-page-context";
import {withDelay} from "../../../../../utils/with-delay";

export function ProcessFlowNodeEditor() {
    const params = useParams();

    const [selectedNode, setSelectedNode] = useState<ProcessDefinitionNodeEntity | null>(null);

    const {
        onSave,
        onDelete,
    } = useContext(ProcessDetailsPageContext);

    const [localNode, setLocalNode] = useState<ProcessDefinitionNodeEntity | null>(null);
    const [layout, setLayout] = useState<GroupLayout | null>(null);

    useEffect(() => {
        // Reset state
        setSelectedNode(null);
        setLocalNode(null);

        // Determine node ID
        const nodeId = parseInt(params.nodeId!);

        // Fetch node details
        new ProcessDefinitionNodeApiService()
            .retrieve(nodeId)
            .then((node) => {
                setSelectedNode(node);
                setLocalNode(null);
            });
    }, [params]);

    useEffect(() => {
        if (selectedNode == null) {
            setLayout(null);
            setLocalNode(null);
            return;
        }

        setLocalNode(selectedNode);
        withDelay(
            new ProcessDefinitionNodeApiService()
                .getConfigurationLayout(selectedNode.id), 500)
            .then(setLayout);
    }, [selectedNode]);

    const handleSaveSelected = () => {
        if (localNode != null) {
            onSave(localNode);
        }
    };

    const handleDeleteSelected = () => {
        if (selectedNode == null) {
            return;
        }

        onDelete(selectedNode);
        // TODO: show error because there are edits
    };

    if (selectedNode == null || layout == null) {
        return (
            <Box>
                <Skeleton height={96}/>
                <Skeleton height={96}/>
                <Skeleton height={256}/>
            </Box>
        );
    }

    return (
        <>
            <TextFieldComponent
                label="Eindeutiger Zugriffsschlüssel"
                value={(localNode ?? selectedNode).dataKey}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        dataKey: val ?? '',
                    });
                }}
                required={true}
                maxCharacters={32}
            />

            <TextFieldComponent
                label="Name"
                value={(localNode ?? selectedNode).name}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        name: val ?? null,
                    });
                }}
                maxCharacters={96}
            />

            <TextFieldComponent
                label="Kurzbeschreibung"
                value={(localNode ?? selectedNode).description}
                onChange={(val) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        description: val ?? null,
                    });
                }}
                multiline={true}
                maxCharacters={512}
            />

            <ElementDerivationContext
                element={layout}
                elementData={(localNode ?? selectedNode).configuration}
                onElementDataChange={(elementData) => {
                    setLocalNode({
                        ...(localNode ?? selectedNode),
                        configuration: elementData,
                    });
                }}
            />

            <Button
                onClick={handleSaveSelected}
            >
                Knoten speichern
            </Button>

            <Button
                onClick={handleDeleteSelected}
            >
                Knoten entfernen
            </Button>
        </>
    );
}
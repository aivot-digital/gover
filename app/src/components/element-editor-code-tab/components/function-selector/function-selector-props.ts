export interface FunctionSelectorProps {
    onSelectFunctionCode: () => void;
    onSelectNoCode: () => void;
    onSelectCloudCode: () => void;
    onSelectNoCodeExpression: () => void;
    allowNoCode: boolean;
    allowExpression: boolean;
    fullWidth: boolean;
}
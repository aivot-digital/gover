import {useState} from 'react';
import {Box, Typography} from '@mui/material';
import {BaseViewProps} from './base-view';
import {
    FunctionInputElement,
    FunctionInputElementItem,
    FunctionInputElementItemType,
    FunctionInputElementReturnType,
} from '../models/elements/form/input/function-input-element';
import {FunctionSelector} from '../components/element-editor-code-tab/components/function-selector/function-selector';
import {CodeEditor} from '../components/code-editor/code-editor';
import {CodeTabConditionSetEditor} from '../components/code-tab-condition-set-editor';
import {ConditionSetOperator} from '../data/condition-set-operator';
import {NoCodeDataType} from '../data/no-code-data-type';
import {NoCodeInputFieldComponent} from '../components/no-code-input-field/no-code-input-field-component';
import {NoCodeEditorDialog} from '../modules/nocode/components/no-code-editor-dialog';
import {LegacyNoCodeWarning} from '../modules/nocode/components/legacy-no-code-warning';
import {LegacyNoCodeReference} from '../modules/nocode/components/legacy-no-code-reference';
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';
import {noCodeEditorElements} from '../modules/nocode/utils/no-code-editor-elements';
import {ElementType} from '../data/element-type/element-type';

const returnTypes: Record<FunctionInputElementReturnType, NoCodeDataType> = {
    STRING: NoCodeDataType.String,
    NUMBER: NoCodeDataType.Number,
    BOOLEAN: NoCodeDataType.Boolean,
    DATE: NoCodeDataType.Date,
    DATETIME: NoCodeDataType.DateTime,
};

export function FunctionInputView(props: BaseViewProps<FunctionInputElement, FunctionInputElementItem>) {
    const {
        element,
        value,
        setValue,
    } = props;
    const {
        rootElement,
        readOnly,
    } = useViewDispatcherContext();
    const [converting, setConverting] = useState(false);
    const locked = Boolean(readOnly || element.disabled || props.isBusy || props.isDeriving);
    const returnType = element.returnType == null ? NoCodeDataType.Runtime : returnTypes[element.returnType];
    const select = (type: FunctionInputElementItemType) => {
        if (!locked) setValue({
            code: null,
            conditionSet: null,
            noCode: null,
            type,
        });
    };

    if (value?.conditionSet != null || value?.type === FunctionInputElementItemType.ConditionSet) {
        const reference = <LegacyNoCodeReference>
            <CodeTabConditionSetEditor element={element}
                                       allElements={noCodeEditorElements([rootElement])}
                                       conditionSet={value.conditionSet ?? {
                                           conditions: [],
                                           conditionsSets: [],
                                           operator: ConditionSetOperator.All,
                                       }}
                                       onChange={() => undefined}
                                       shouldReturnString={false}
                                       editable={false}
                                       presentation="reference"/>
        </LegacyNoCodeReference>;
        return <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
        }}>
            <Typography variant="subtitle1">{element.label ?? 'Funktion'}</Typography>
            <LegacyNoCodeWarning onStart={!locked ? () => setConverting(true) : undefined}/>
            {!converting && reference}
            {converting && <NoCodeEditorDialog title="Regel umstellen"
                                               rootElement={rootElement}
                                               returnType={returnType}
                                               value={null}
                                               required
                                               // Applying replaces the legacy rule, which must not be lost to an unfinished draft.
                                               requireComplete
                                               readOnly={locked}
                                               reference={<>
                                                   <Typography sx={{
                                                       mb: 2,
                                                       maxWidth: 820,
                                                   }}>Die bisherige Regel bleibt erhalten, bis Sie die neue Fassung in
                                                      diesem Dialog übernehmen.</Typography>
                                                   {reference}
                                                   <Typography variant="h6"
                                                               component="h3"
                                                               sx={{mt: 2}}>Neue Fassung</Typography>
                                                   <Typography sx={{mt: 0.5}}>Bilden Sie die bisherige Regel nach und
                                                                              vergleichen Sie die
                                                                              Bedingungen.</Typography>
                                               </>}
                                               contextType={rootElement.type === ElementType.FormLayout ? 'FORM' : 'PROCESS'}
                                               onClose={() => setConverting(false)}
                                               onApply={(noCode) => {
                                                   if (locked) return;
                                                   setValue({
                                                       type: FunctionInputElementItemType.NoCode,
                                                       noCode,
                                                       code: null,
                                                       conditionSet: null,
                                                   });
                                                   setConverting(false);
                                               }}/>}
        </Box>;
    }
    if (value?.type == null) return <Box component="fieldset"
                                         disabled={locked}
                                         sx={{
                                             border: 0,
                                             p: 0,
                                             m: 0,
                                             minWidth: 0,
                                         }}>
        <FunctionSelector onSelectFunctionCode={() => select(FunctionInputElementItemType.Javascript)}
                          onSelectNoCode={() => undefined}
                          allowNoCode={false}
                          onSelectCloudCode={() => select(FunctionInputElementItemType.Javascript)}
                          onSelectNoCodeExpression={() => select(FunctionInputElementItemType.NoCode)}
                          allowExpression
                          fullWidth/>
    </Box>;
    if (value.type === FunctionInputElementItemType.Javascript) return <CodeEditor actions={[]}
                                                                                   value={value.code?.code}
                                                                                   disabled={locked}
                                                                                   onChange={(code) => {
                                                                                       if (!locked) setValue({
                                                                                           ...value,
                                                                                           code: {
                                                                                               ...value.code,
                                                                                               code,
                                                                                           },
                                                                                       });
                                                                                   }}/>;
    return <NoCodeInputFieldComponent rootElement={rootElement}
                                      label={element.label ?? 'Ausdruck'}
                                      hint={element.hint}
                                      error={props.errors?.join(' ')}
                                      errorDetails={props.errorDetails}
                                      required={element.required}
                                      disabled={locked}
                                      value={{noCode: value.noCode}}
                                      desiredReturnType={returnType}
                                      onChange={(next) => {
                                          if (!locked) setValue({
                                              ...value,
                                              noCode: next?.noCode ?? null,
                                          });
                                      }}/>;
}

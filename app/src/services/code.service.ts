import {ApiConfig} from '../api-config';
import axios from 'axios';
import {CrudService} from './crud.service';
import {Application} from '../models/entities/application';
import {AnyElement} from '../models/elements/any-element';
import {isAnyElementWithChildren} from '../models/elements/any-element-with-children';
import {isStringNullOrEmpty} from "../utils/string-utils";

const scriptElementIdPrefix = 'logic-bundle-';

export class CodeService {
    public static getCodeLink(id: number) {
        return ApiConfig.address + '/public/code/' + id;
    }

    public static async getCodeString(id: number) {
        return await axios.get(CodeService.getCodeLink(id))
            .then(response => response.data);
    }

    public static async uploadCode(id: number, file: File): Promise<void> {
        const form = new FormData();
        form.append('file', file);
        return await axios.post(
            ApiConfig.address + '/code/' + id,
            form,
            CrudService.getConfig()
        );
    }

    static async loadCode(id: number): Promise<any> {
        return new Promise<void>((resolve, reject) => {
            let script = document.getElementById(scriptElementIdPrefix + id) as HTMLScriptElement;
            if (script != null) {
                script.remove();
            }
            script = document.createElement('script');
            script.src = CodeService.getCodeLink(id);
            script.id = scriptElementIdPrefix + id;
            document.body.appendChild(script);
            script.onload = () => {
                resolve();
            };
            script.onerror = (err) => {
                reject(err);
            };
        });
    }

    // public static createCodeStubs(application: Application): string {
    //     const root = application.root;
    //     const lines = [
    //         '// Code for ' + root.title,
    //         '',
    //     ];
    //
    //     const processElement = (element: AnyElement) => {
    //         const addCodeBlock = (
    //             field: 'validate' | 'patch' | 'visibility',
    //             funcPrefix: string,
    //             funcPurpose: string,
    //             funcReturnType: string,
    //             funcReturnText: string,
    //             funcDefaultReturn: string,
    //         ) => {
    //             const data: FunctionSet | undefined = element[field];
    //             if (data != null && (!isStringNullOrEmpty(data.requirements) || !isStringNullOrEmpty(data.functionName))) {
    //                 const funcName = isStringNullOrEmpty(data.functionName) ? funcPrefix + '_' + element.id : data.functionName;
    //
    //                 lines.push('/**');
    //                 lines.push(` * ${funcPurpose}`);
    //                 lines.push(' *');
    //                 lines.push(' * @param {any} $global - The global data object containing all customer data.');
    //                 lines.push(' * @param {any} $element - The config data of the current element.');
    //                 lines.push(' * @param {string} $id - The id of the current element.');
    //                 lines.push(` * @returns {${funcReturnType}} ${funcReturnText}`);
    //                 lines.push(' */');
    //                 lines.push(`function ${funcName}($global, $element, $id) {`);
    //                 if (!isStringNullOrEmpty(data.requirements)) {
    //                     lines.push('    /*');
    //                     lines.push('    ' + data.requirements);
    //                     lines.push('    */');
    //                 }
    //                 lines.push(`    return ${funcDefaultReturn};`);
    //                 lines.push('}\n');
    //             }
    //         };
    //
    //         addCodeBlock(
    //             'validate',
    //             'validate',
    //             `Determine if ${element.id} is valid and return possible errors.`,
    //             '(string|null)',
    //             'The error string if the element is not valid or null if no error was found.',
    //             'null'
    //         );
    //
    //         addCodeBlock(
    //             'visibility',
    //             'is_visible',
    //             `Determine if ${element.id} is visible.`,
    //             'boolean',
    //             'True if the element is visible, false otherwise.',
    //             'false'
    //         );
    //
    //         addCodeBlock(
    //             'patch',
    //             'patch',
    //             `Patch the config of ${element.id} with custom data.`,
    //             'any',
    //             'A patches subset of the element config.',
    //             '{...$element}'
    //         );
    //
    //         if (isAnyElementWithChildren(element)) {
    //             element.children.forEach(processElement);
    //         }
    //     };
    //
    //     processElement(root);
    //
    //     return lines.join('\n');
    // }
}

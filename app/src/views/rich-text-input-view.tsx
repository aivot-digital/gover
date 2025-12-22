import {BaseViewProps} from "./base-view";
import {Box} from "@mui/material";
import {RichTextInputElement} from "../models/elements/form/input/rich-text-input-element";
import {
    BlockTypeSelect,
    BoldItalicUnderlineToggles,
    CodeToggle,
    CreateLink,
    headingsPlugin,
    listsPlugin,
    ListsToggle,
    markdownShortcutPlugin,
    MDXEditor,
    quotePlugin,
    toolbarPlugin,
    UndoRedo,
} from '@mdxeditor/editor';
import '@mdxeditor/editor/style.css';
import Typography from "@mui/material/Typography";

export function RichTextView(props: BaseViewProps<RichTextInputElement, string>) {
    const {
        element,
        value,
        setValue,
    } = props;

    return (
        <Box>
            <Typography
                sx={{
                    marginBottom: 1,
                    fontWeight: 'medium',
                }}
            >
                {element.label}{element.required ? ' *' : ''}
            </Typography>

            <Box
                sx={{
                    border: '1px solid #ccc',
                }}
            >
                <MDXEditor
                    markdown={value ?? ''}
                    onChange={(val) => {
                        setValue(val ?? null);
                    }}
                    plugins={[
                        headingsPlugin(),
                        quotePlugin(),
                        listsPlugin(),
                        toolbarPlugin({
                            toolbarContents: () => (
                                <>
                                    <UndoRedo/>
                                    <BoldItalicUnderlineToggles/>
                                    <BlockTypeSelect/>
                                    <CodeToggle/>
                                    <CreateLink/>
                                    <ListsToggle/>
                                </>
                            )
                        }),
                        markdownShortcutPlugin(),
                    ]}
                />
            </Box>

            <Typography
                sx={{
                    marginTop: 1,
                    color: 'text.secondary',
                }}
                variant="caption"
            >
                {element.hint}
            </Typography>
        </Box>
    );
}
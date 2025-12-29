import Typography from "@mui/material/Typography";
import {Box, SxProps} from "@mui/material";
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
    UndoRedo
} from "@mdxeditor/editor";
import {isStringNullOrEmpty} from "../../utils/string-utils";
import '@mdxeditor/editor/style.css';

interface RichTextInputComponentProps {
    label: string;
    hint?: string | null | undefined;
    required?: boolean | null | undefined;
    value: string | null | undefined;
    onChange: (value: string | null) => void;
    sx?: SxProps | null | undefined;
}

export function RichTextInputComponent(props: RichTextInputComponentProps) {
    const {
        label,
        hint,
        required,
        value,
        onChange,
        sx,
    } = props;

    return (
        <Box
            sx={sx}
        >
            <Typography
                sx={{
                    marginBottom: 1,
                    fontWeight: 'medium',
                }}
            >
                {label}{required ? ' *' : ''}
            </Typography>

            <Box
                sx={{
                    border: '1px solid #ccc',
                }}
            >
                <MDXEditor
                    markdown={value ?? ''}
                    onChange={(val) => {
                        if (isStringNullOrEmpty(val)) {
                            onChange(null);
                            return;
                        }
                        onChange(val ?? null);
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

            {
                hint != null &&
                <Typography
                    sx={{
                        marginTop: 1,
                        color: 'text.secondary',
                    }}
                    variant="caption"
                >
                    {hint}
                </Typography>
            }
        </Box>
    );
}
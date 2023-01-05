import React, {useCallback} from 'react';
import {Editor} from '@tiptap/react';
import {Button, ButtonGroup} from '@mui/material';
import {
    faBold,
    faItalic,
    faLink,
    faLinkSlash,
    faListOl,
    faListUl,
    faStrikethrough,
    faX
} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {IconDefinition} from '@fortawesome/pro-duotone-svg-icons';


export function RichTextEditorMenuComponentView({editor}: { editor: Editor | null }) {
    const fontOptions: {
        icon: IconDefinition;
        style?: string;
        func: () => void;
    }[] = [
        {
            icon: faBold,
            style: 'bold',
            func: () => editor?.chain().focus().toggleBold().run()
        },
        {
            icon: faItalic,
            style: 'italic',
            func: () => editor?.chain().focus().toggleItalic().run()
        },
        {
            icon: faStrikethrough,
            style: 'strike',
            func: () => editor?.chain().focus().toggleStrike().run()
        },
        {
            icon: faX,
            func: () => editor?.chain().focus().unsetAllMarks().run()
        },
    ];

    const parOptions: {
        icon: IconDefinition;
        style: string;
        func: () => any;
    }[] = [
        {
            icon: faListUl,
            style: 'bulletList',
            func: () => editor?.chain().focus().toggleBulletList().run()
        },
        {
            icon: faListOl,
            style: 'orderedList',
            func: () => editor?.chain().focus().toggleOrderedList().run()
        },
    ];

    const setLink = useCallback(() => {
        if (editor) {
            const previousUrl = editor.getAttributes('link').href
            let url = window.prompt('URL', previousUrl)

            // cancelled
            if (url === null) {
                return
            }

            // empty
            if (url === '') {
                editor.chain().focus().extendMarkRange('link').unsetLink()
                    .run()

                return
            }

            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                url = 'http://' + url;
            }

            // update link
            editor.chain().focus().extendMarkRange('link').setLink({href: url})
                .run()
        }
    }, [editor]);

    if (!editor) {
        return null
    }

    return (
        <>
            <ButtonGroup>
                {
                    fontOptions.map((option, index) => (
                        <Button
                            key={index}
                            onClick={option.func}
                            variant={option.style != null && editor.isActive(option.style) ? 'contained' : 'outlined'}
                        >
                            <FontAwesomeIcon icon={option.icon} />
                        </Button>
                    ))
                }
            </ButtonGroup>

            <ButtonGroup sx={{ml: 1}}>
                {
                    parOptions.map((option, index) => (
                        <Button
                            key={index}
                            onClick={option.func}
                            variant={editor.isActive(option.style) ? 'contained' : 'outlined'}
                        >
                            <FontAwesomeIcon icon={option.icon} />
                        </Button>
                    ))
                }
            </ButtonGroup>

            <ButtonGroup sx={{ml: 1}}>
                <Button
                    onClick={setLink}
                    variant={editor.isActive('link') ? 'contained' : 'outlined'}
                >
                    <FontAwesomeIcon icon={faLink} />
                </Button>
                <Button
                    onClick={() => editor.chain().focus().unsetLink().run()}
                    disabled={!editor.isActive('link')}
                >
                    <FontAwesomeIcon icon={faLinkSlash} />
                </Button>
            </ButtonGroup>
        </>
    )
}
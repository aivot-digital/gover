import * as React from "react";
import MuiChip, { type ChipProps as MuiChipProps } from "@mui/material/Chip";
import { alpha, styled } from "@mui/material/styles";

type ChipMode = "solid" | "soft";
type SupportedColor = Exclude<MuiChipProps["color"], undefined | "inherit">;

export type ChipProps = Omit<MuiChipProps, "variant"> & {
    mode?: ChipMode;
};

const ChipRoot = styled(MuiChip, {
    shouldForwardProp: (prop) => prop !== "mode" && prop !== "colorProp",
})<{
    mode: ChipMode;
    colorProp: SupportedColor;
}>(({ theme, mode, colorProp }) => {
    if (mode !== "soft") return {};

    const main =
        colorProp === "default"
            ? theme.palette.text.secondary
            : theme.palette[colorProp].main;

    return {
        backgroundColor: alpha(main, 0.08),
        border: `1px solid ${alpha(main, 0.16)}`,
        color: main,

        "& .MuiChip-icon": {
            color: alpha(main, 0.9),
        },

        "& .MuiChip-deleteIcon": {
            color: alpha(main, 0.7),
            "&:hover": { color: main },
        },
    };
});

export function Chip(props: ChipProps) {
    const { mode = "solid", color = "default", ...rest } = props;

    const colorProp = (color ?? "default") as SupportedColor;

    return (
        <ChipRoot
            {...rest}
            // Important: In soft mode, disable MUI color variant, otherwise e.g. white text
            color={mode === "soft" ? "default" : colorProp}
            mode={mode}
            colorProp={colorProp}
        />
    );
}
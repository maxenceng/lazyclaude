// PROTOTYPE: variants E, F, G and H (F's left column with G's main column), three Base tab layouts on D's window-wide system. Not shipped.
import type { VariantProps } from './main';
import { FramedWindow } from './VariantB';
import './variantD.css';
import './variantLayouts.css';

export const VariantE = (props: VariantProps) => <FramedWindow {...props} chosen layout="e" />;
export const VariantF = (props: VariantProps) => <FramedWindow {...props} chosen layout="f" />;
export const VariantG = (props: VariantProps) => <FramedWindow {...props} chosen layout="g" />;
export const VariantH = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" />;

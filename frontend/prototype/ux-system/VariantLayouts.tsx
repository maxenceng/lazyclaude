// PROTOTYPE: variants E, F, G and H (F's left column with G's main column), three Base tab layouts on D's window-wide system. Not shipped.
import type { VariantProps } from './main';
import { FramedWindow } from './VariantB';
import './variantD.css';
import './variantLayouts.css';
import './variantBoardTab.css';

export const VariantE = (props: VariantProps) => <FramedWindow {...props} chosen layout="e" />;
export const VariantF = (props: VariantProps) => <FramedWindow {...props} chosen layout="f" />;
export const VariantG = (props: VariantProps) => <FramedWindow {...props} chosen layout="g" />;
export const VariantH = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" />;
export const VariantI = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" boardLayout="i" />;
export const VariantJ = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" boardLayout="j" />;
export const VariantK = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" boardLayout="k" />;
export const VariantR0 = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" reviewLayout="r0" />;
export const VariantR1 = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" reviewLayout="r1" />;
export const VariantR2 = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" reviewLayout="r2" />;
export const VariantR3 = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" reviewLayout="r3" />;
export const VariantR4 = (props: VariantProps) => <FramedWindow {...props} chosen layout="h" reviewLayout="r4" />;

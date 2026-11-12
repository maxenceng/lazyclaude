// PROTOTYPE: variant D, "Chosen". B's look, pane actions, quit and status line; A's lit title and selection; whole rows and live keys only. Not shipped.
import type { VariantProps } from './main';
import { FramedWindow } from './VariantB';
import './variantD.css';

export const VariantD = (props: VariantProps) => <FramedWindow {...props} chosen />;

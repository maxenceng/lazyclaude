import styles from './TypeTag.module.css';

/** The colours a ticket's type can carry; a type with none of its own is dim. */
export enum TypeColour {
  Feature = 'feature',
  Fix = 'fix',
  Chore = 'chore',
}

/** A ticket's type as its tag shows it: the type's word, and the colour its owner gave it, if any. */
export interface TicketTypeTag {
  name: string;
  colour: TypeColour | undefined;
}

/** A ticket's type, as a word in the colour its owner chose for it, or dim without one. */
export const TypeTag = ({ name, colour }: TicketTypeTag) => {
  return (
    <span className={styles.tag} data-type={colour ?? 'other'}>
      {name}
    </span>
  );
};

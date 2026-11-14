import type { ReactNode } from 'react';
import { Ci } from './CiGlyph';
import styles from './Table.module.css';

/** How a column takes its room: `fit` as wide as its widest cell, `trim` that up to a limit then cut with an ellipsis, `fill` whatever is left. */
export enum ColumnSize {
  Fit = 'fit',
  Trim = 'trim',
  Fill = 'fill',
}

/** How a cell reads: plain, or dim for what a row carries beside its subject. */
export enum CellTone {
  Plain = 'plain',
  Dim = 'dim',
}

export interface ColumnHead {
  label: string;
  size: ColumnSize;
}

interface TableProps {
  /** What the table lists, for a screen reader. */
  label: string;
  heads: readonly ColumnHead[];
  children: ReactNode;
}

/** A full-width table: a header row of small capitals over its rows. */
export const Table = ({ label, heads, children }: TableProps) => {
  return (
    <table className={styles.table} aria-label={label}>
      <thead>
        <tr>
          {heads.map((head) => (
            <th key={head.label} scope="col" className={styles[head.size]}>
              {head.label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>{children}</tbody>
    </table>
  );
};

const STRIPES: Readonly<Record<Ci, string>> = { failed: 'failed', pending: 'pending', passed: 'passed' };

interface RowProps {
  /** A coloured edge on the row's left, as one more sign of the state the row's own words give. */
  stripe: Ci | undefined;
  selected: boolean;
  onSelect: () => void;
  children: ReactNode;
}

/** One row of a table: its selection is the pane's cursor, a press on it selects it. */
export const TableRow = ({ stripe, selected, onSelect, children }: RowProps) => {
  return (
    <tr
      data-row=""
      data-stripe={stripe === undefined ? 'none' : STRIPES[stripe]}
      className={[styles.row, selected ? styles.selected : ''].join(' ')}
      aria-current={selected ? 'true' : undefined}
      onClick={onSelect}
    >
      {children}
    </tr>
  );
};

interface CellProps {
  size: ColumnSize;
  /** `dim` for what a row carries beside its subject; `mono` for an identifier. */
  tone?: CellTone;
  mono?: boolean;
  /** A small dim note after the cell's content, such as a number after a title. */
  note?: string;
  children: ReactNode;
}

/** One cell of a row, in the column of its size. */
export const Cell = ({ size, tone = CellTone.Plain, mono = false, note, children }: CellProps) => {
  return (
    <td className={[styles.cell, styles[size], tone === CellTone.Dim ? styles.dim : '', mono ? styles.mono : ''].join(' ')}>
      {children}
      {note === undefined ? null : <small className={styles.note}>{note}</small>}
    </td>
  );
};

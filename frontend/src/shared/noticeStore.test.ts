import { describe, expect, it } from 'vitest';
import { NoticeStore } from './noticeStore';

describe('NoticeStore', () => {
  it('shows nothing until a notice is raised', () => {
    expect(new NoticeStore().notice.get()).toBeUndefined();
  });

  it('shows the last notice raised until it is cleared', () => {
    const store = new NoticeStore();
    store.raise('Could not open /p/x: no such file');
    store.raise('No heading "a" in b.md; opened at its top.');
    expect(store.notice.get()).toBe('No heading "a" in b.md; opened at its top.');
    store.clear();
    expect(store.notice.get()).toBeUndefined();
  });
});

/** The one place mermaid.js is imported, on demand, so Vite splits it into a chunk only a document with a diagram fetches. */
export const loadMermaid = () => import('mermaid');

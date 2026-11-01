import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { createApp } from './app';
import './design-system/global.css';
import { createClient, tauriRelay } from './protocol';

const app = createApp(createClient(tauriRelay));
app.start();
void app.client.connect();

// deferred: opening a ticket's session does nothing yet — close it when the hosted session pane is built.
const openSession = () => {};

const root = document.getElementById('root');
if (root === null) {
  throw new Error('index.html has no #root');
}
createRoot(root).render(
  <StrictMode>
    <App app={app} packaged={import.meta.env.PROD} onOpen={openSession} />
  </StrictMode>,
);

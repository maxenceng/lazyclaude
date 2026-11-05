import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { createApp } from './app';
import './design-system/global.css';
import './i18n/i18n';
import { browserPlatform } from './platform';
import { createClient, tauriRelay } from './protocol';

const app = createApp(createClient(tauriRelay), browserPlatform());
app.start();
void app.client.connect();

const root = document.getElementById('root');
if (root === null) {
  throw new Error('index.html has no #root');
}
createRoot(root).render(
  <StrictMode>
    <App app={app} packaged={import.meta.env.PROD} />
  </StrictMode>,
);

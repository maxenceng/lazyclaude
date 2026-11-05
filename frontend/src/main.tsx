import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { AppModel } from './app';
import './design-system/global.css';
import './i18n/i18n';
import { BrowserPlatform } from './platform';
import { ProtocolClient, tauriRelay } from './protocol';
import { Heartbeat } from './window/heartbeat';
import { pageAlive } from './window/tauri';

const HEARTBEAT_MILLISECONDS = 1000;

const platform = new BrowserPlatform();
const app = new AppModel(new ProtocolClient(tauriRelay), platform);
app.start();
void app.client.connect();
new Heartbeat(pageAlive, HEARTBEAT_MILLISECONDS);

const root = document.getElementById('root');
if (root === null) {
  throw new Error('index.html has no #root');
}
void platform
  .terminalFontLoaded()
  .catch(() => undefined)
  .then(() =>
    createRoot(root).render(
      <StrictMode>
        <App app={app} packaged={import.meta.env.PROD} />
      </StrictMode>,
    ),
  );

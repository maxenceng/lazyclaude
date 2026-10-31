/** Sidecar: the backend a desktop frontend drives over JSON-RPC on stdin and stdout, composing the contexts' primary adapters as the shell does (ADR 0050). */
@ApplicationModule(type = SHELL)
package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.ModuleType.SHELL;

import dev.maxenceng.lazyclaude.ApplicationModule;

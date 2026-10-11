/**
 * Shared kernel: types more than one context's domain needs (ADR 0010), and the warnings those
 * contexts raise (Warning, WarningPort, WarnOnce), read by the shell through WarningQuery and raised
 * for a stopped thread through WarningCommand (ADR 0022).
 * Keep this small — anything that belongs to one context belongs in that context.
 */
@ApplicationModule(type = KERNEL)
package dev.maxenceng.lazyclaude.shared;

import static dev.maxenceng.lazyclaude.ModuleType.KERNEL;

import dev.maxenceng.lazyclaude.ApplicationModule;

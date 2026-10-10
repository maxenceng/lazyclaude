/**
 * Error kernel: the base of every domain failure and the input assertions a domain makes.
 * Keep this small — anything that belongs to one context belongs in that context.
 */
@ApplicationModule(type = KERNEL)
package dev.maxenceng.lazyclaude.error;

import static dev.maxenceng.lazyclaude.ModuleType.KERNEL;

import dev.maxenceng.lazyclaude.ApplicationModule;

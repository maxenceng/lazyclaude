package dev.tamboui.tui;

public final class RenderThreadForTests {

    private RenderThreadForTests() {}

    public static void runAsRenderThread(Runnable render) {
        RenderThread.markAsRenderThread();
        try {
            render.run();
        } finally {
            RenderThread.clearRenderThread();
        }
    }
}

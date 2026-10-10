package dev.maxenceng.lazyclaude.board.published;

/** One ticket as another module reads it; {@code context} and {@code type} are empty when the file has none. */
public record TicketView(String id, String title, String status, String context, String type) {}

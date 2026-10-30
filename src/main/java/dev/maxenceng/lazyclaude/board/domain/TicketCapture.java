package dev.maxenceng.lazyclaude.board.domain;

/** What capturing a ticket came to: GitHub took it, or it did not. */
public sealed interface TicketCapture permits CapturedTicket, RefusedTicketCapture {}

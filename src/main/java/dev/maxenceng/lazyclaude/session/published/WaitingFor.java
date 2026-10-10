package dev.maxenceng.lazyclaude.session.published;

/** Why a waiting session waits, or nothing when its source gives no reason. */
public sealed interface WaitingFor permits WaitingReason, NoWaitingReason {}

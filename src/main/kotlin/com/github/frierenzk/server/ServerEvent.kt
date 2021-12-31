package com.github.frierenzk.server

import com.github.frierenzk.dispatcher.EventType

enum class ServerEvent : EventType {
    Default,
    BroadCast,
    TaskFinish
}
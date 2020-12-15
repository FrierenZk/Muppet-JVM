package com.github.frierenzk.server

import com.github.frierenzk.dispatcher.EventType

enum class ServerEvent : EventType {
    Default,
    Status,
    AddTask,
    AvailableList,
    WaitingList,
    WorkingList,
    BroadCast,
    UpdateList,
    TaskFinish
}
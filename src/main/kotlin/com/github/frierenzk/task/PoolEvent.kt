package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.EventType

enum class PoolEvent : EventType {
    Default,
    AddTask,
    CreateTask,
    StopTask,
    WorkingList,
    WaitingList,
    AvailableList,
    TaskStatus,
    ReloadConfig
}
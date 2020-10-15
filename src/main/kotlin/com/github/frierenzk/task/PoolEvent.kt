package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.EventType

enum class PoolEvent:EventType {
    Default,
    AddTask,
    StopTask,
    WorkingList,
    WaitingList,
    AvailableList,
    TaskStatus,
    ReloadConfig
}
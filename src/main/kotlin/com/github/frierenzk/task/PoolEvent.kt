package com.github.frierenzk.task

import com.github.frierenzk.dispatcher.EventType

enum class PoolEvent : EventType {
    Default,
    CreateTask,
    ProcessingList,
    StopTask,
    GetTaskStatus,
    GetTaskName,
    GetTaskConfig,
}
package com.github.frierenzk.task

enum class TaskStatus {
    Waiting,
    Working,
    Stopping,
    Finished,
    Error,
    Null;

    fun isEnd() = isFinished() || isError() || isStopping()
    fun isWaiting() = this == Waiting
    fun isWorking() = this == Working
    fun isStopping() = this == Stopping
    fun isFinished() = this == Finished
    fun isError() = this == Error
}
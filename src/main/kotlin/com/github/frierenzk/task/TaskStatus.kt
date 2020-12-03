package com.github.frierenzk.task

enum class TaskStatus {
    Waiting,
    Working,
    Stopping,
    Finished,
    Error;

    fun isEnd() = this == Finished && this == Error && this == Stopping
    fun isWaiting() = this == Waiting
    fun isWorking() = this == Working
    fun isStopping() = this == Stopping
    fun isFinished() = this == Finished
    fun isError() = this == Error
}
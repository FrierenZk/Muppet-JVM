package com.github.frierenzk.config

import com.github.frierenzk.dispatcher.EventType

enum class ConfigEvent : EventType {
    Default,
    Reload,
    Save,
    GetConfig,
    GetConfigList,
    AddConfig,
    ModifyConfig,
    DeleteConfig,
    GetTicker,
    AddTicker,
    ModifyTicker,
    DeleteTicker
}
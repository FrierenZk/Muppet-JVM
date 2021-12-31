package com.github.frierenzk.config

import com.github.frierenzk.dispatcher.EventType

enum class ConfigEvent : EventType {
    Default,
    Reload,
    Save,
    GetConfig,
    GetConfigList,
    GetRelativeConfig,
    AddConfig,
    ModifyConfig,
    DeleteConfig,
    GetTickerList,
    GetTicker,
    AddTicker,
    ModifyTicker,
    DeleteTicker
}
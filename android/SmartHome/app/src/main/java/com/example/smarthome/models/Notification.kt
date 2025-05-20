package com.example.smarthome.models

data class Notification(
    var id: String? = null,
    var title: String? = null,
    var message: String? = null,
    var timestamp: String? = null,
    var type: String? = null,
    var severity: String? = null,
    var gas_level: Int? = null,
    var read: Boolean? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null)
} 
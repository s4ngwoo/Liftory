package com.example.domain.repository

interface SyncListener {
    fun startListening()
    fun stopListening()
}

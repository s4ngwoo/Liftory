package com.example.infrastructure.clock

import android.os.SystemClock
import com.example.domain.port.MonotonicClock

class SystemMonotonicClock : MonotonicClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

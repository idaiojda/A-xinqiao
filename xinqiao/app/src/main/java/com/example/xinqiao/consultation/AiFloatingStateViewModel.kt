package com.example.xinqiao.consultation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AiFloatingStateViewModel(private val handle: SavedStateHandle) : ViewModel() {
    private val _isExpanded = MutableStateFlow(handle.get<Boolean>(KEY_EXPANDED) ?: false)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    private val _isMinimized = MutableStateFlow(handle.get<Boolean>(KEY_MINIMIZED) ?: true)
    val isMinimized: StateFlow<Boolean> = _isMinimized

    private val _floatX = MutableStateFlow(handle.get<Int>(KEY_FLOAT_X) ?: -1)
    val floatX: StateFlow<Int> = _floatX

    private val _floatY = MutableStateFlow(handle.get<Int>(KEY_FLOAT_Y) ?: -1)
    val floatY: StateFlow<Int> = _floatY

    private val _scale = MutableStateFlow(handle.get<Float>(KEY_SCALE) ?: 1f)
    val scale: StateFlow<Float> = _scale

    private val _winW = MutableStateFlow(handle.get<Int>(KEY_WIN_W) ?: 0)
    val winW: StateFlow<Int> = _winW

    private val _winH = MutableStateFlow(handle.get<Int>(KEY_WIN_H) ?: 0)
    val winH: StateFlow<Int> = _winH

    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
        handle[KEY_EXPANDED] = expanded
    }
    fun setMinimized(minimized: Boolean) {
        _isMinimized.value = minimized
        handle[KEY_MINIMIZED] = minimized
    }
    fun setFloatPos(x: Int, y: Int) {
        _floatX.value = x
        _floatY.value = y
        handle[KEY_FLOAT_X] = x
        handle[KEY_FLOAT_Y] = y
    }
    fun setScale(s: Float) {
        _scale.value = s
        handle[KEY_SCALE] = s
    }
    fun setWindowSize(w: Int, h: Int) {
        _winW.value = w
        _winH.value = h
        handle[KEY_WIN_W] = w
        handle[KEY_WIN_H] = h
    }

    companion object {
        const val KEY_EXPANDED = "ai_float_expanded"
        const val KEY_MINIMIZED = "ai_float_minimized"
        const val KEY_FLOAT_X = "ai_float_x"
        const val KEY_FLOAT_Y = "ai_float_y"
        const val KEY_SCALE = "ai_float_scale"
        const val KEY_WIN_W = "ai_float_win_w"
        const val KEY_WIN_H = "ai_float_win_h"
    }
}
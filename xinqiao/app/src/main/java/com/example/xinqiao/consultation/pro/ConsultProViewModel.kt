package com.example.xinqiao.consultation.pro

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConsultProViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ConsultRepository(app)

    private val _consultants = MutableStateFlow<List<Consultant>>(emptyList())
    val consultants: StateFlow<List<Consultant>> = _consultants

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var page = 1
    private val size = 10

    // 过滤条件
    private val sp by lazy { app.getSharedPreferences("consult_filters", Context.MODE_PRIVATE) }
    var field: String? = sp.getString("field", "全部")
    var mode: String? = sp.getString("mode", "全部")
    var sort: String? = sp.getString("sort", "综合评分")

    fun setFilters(field: String?, mode: String?, sort: String?) {
        this.field = field
        this.mode = mode
        this.sort = sort
        sp.edit().putString("field", field).putString("mode", mode).putString("sort", sort).apply()
    }

    fun refresh(token: String?) {
        page = 1
        loadInternal(reset = true, token = token)
    }

    fun loadMore(token: String?) {
        if (_loading.value) return
        page += 1
        loadInternal(reset = false, token = token)
    }

    private fun loadInternal(reset: Boolean, token: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null
            val result = repo.fetchConsultants(normalize(field), normalize(mode), normalize(sort), page, size, token)
            result.onSuccess { list ->
                _consultants.value = if (reset) list else _consultants.value + list
            }.onFailure { e ->
                _error.value = e.message
                page = maxOf(1, page - 1)
            }
            _loading.value = false
        }
    }

    private fun normalize(s: String?): String? {
        return when (s) {
            null, "全部" -> null
            else -> s
        }
    }
}
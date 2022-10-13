package com.depromeet.housekeeper.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.depromeet.housekeeper.model.response.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel: ViewModel() {
    private val _networkError: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val networkError: StateFlow<Boolean>
        get() = _networkError

    fun setNetworkError(value: Boolean) {
        _networkError.value = value
    }

    fun <T : Any> receiveApiResult(result: ApiResult<T>): T? {
        when (result) {
            is ApiResult.Success -> {
                return result.value
            }
            else ->{
                setNetworkError(true)
            }
        }
        return null
    }
}
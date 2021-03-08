package com.aglushkov.wordteacher.androidApp.general.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.aglushkov.wordteacher.androidApp.R
import com.aglushkov.wordteacher.androidApp.databinding.LoadingStatusViewBinding
import com.aglushkov.wordteacher.androidApp.general.extensions.getLayoutInflater
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.isLoadedAndEmpty
import com.aglushkov.wordteacher.shared.general.resource.isLoading

class LoadingStatusView: FrameLayout {
    private var binding: LoadingStatusViewBinding
    var needChangeVisibility = false

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        applyAttributeSet(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        applyAttributeSet(attrs)
    }

    init {
        binding = LoadingStatusViewBinding.inflate(context.getLayoutInflater(), this, true)
    }

    private fun applyAttributeSet(attrs: AttributeSet?) {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.LoadingStatusView) {
                binding.text.text = getString(R.styleable.LoadingStatusView_text)
                needChangeVisibility = getBoolean(R.styleable.LoadingStatusView_need_change_visibility, false)
            }
        }
    }

    fun showProgress(aText: String? = null) {
        if (needChangeVisibility) isVisible = true
        binding.progress.visibility = View.VISIBLE
        binding.text.visibility = if (aText != null) View.VISIBLE else View.GONE
        binding.text.text = aText
        binding.tryAgainButton.isVisible = false
    }

    fun showText(aText: String?, showTryAgainButton: Boolean = false) {
        if (needChangeVisibility) isVisible = true
        binding.progress.visibility = View.INVISIBLE
        binding.text.visibility = if (aText != null) View.VISIBLE else View.GONE
        binding.text.text = aText
        binding.tryAgainButton.isVisible = showTryAgainButton
    }

    fun hideElements() {
        if (needChangeVisibility) isVisible = false
        binding.progress.visibility = View.INVISIBLE
        binding.text.visibility = View.GONE
        binding.text.text = null
        binding.tryAgainButton.isVisible = false
    }

    fun setOnTryAgainListener(callback: (View) -> Unit) {
        binding.tryAgainButton.setOnClickListener(callback)
    }
}

fun <T> Resource<T>.bind(view: LoadingStatusView?,
                         errorText: String? = null,
                         loadingText: String? = null,
                         emptyText: String?) where T : Collection<*> {
    if (view == null) return

    if (isLoadedAndEmpty() && emptyText != null) {
        view.showText(emptyText, false)
    } else {
        bind(view, errorText, loadingText)
    }
}

fun <T> Resource<T>.bind(view: LoadingStatusView?,
                         errorText: String? = null,
                         loadingText: String? = null) {
    if (view == null) return

    val data = data()
    val isEmptyList = data is List<*> && data.isEmpty()
    if (data != null && !isEmptyList) {
        view.hideElements()
    } else {
        if (isLoading()) {
            view.showProgress(loadingText)
        } else {
            if (this is Resource.Error) {
                view.showText(errorText, canTryAgain)
            } else {
                view.hideElements()
            }
        }
    }
}
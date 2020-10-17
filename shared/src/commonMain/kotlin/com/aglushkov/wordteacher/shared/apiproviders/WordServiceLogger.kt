package com.aglushkov.wordteacher.shared.apiproviders

import com.aglushkov.wordteacher.shared.general.Logger
import com.aglushkov.wordteacher.shared.general.e
import com.aglushkov.wordteacher.shared.general.v
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

class WordServiceLogger(
    private val serviceTag: String
) {
    fun logLoadingStarted(word: String) {
        Logger.v("Loading definition for: $word", tag = serviceTag)
    }

    fun logLoadingCompleted(
        word: String,
        response: HttpResponse,
        stringResponse: String
    ) {
        if (response.status == HttpStatusCode.OK) {
            Logger.v("Loaded definition for: $word", tag = serviceTag)
        } else {
            Logger.e("Status: ${response.status} response: $stringResponse", tag = serviceTag)
        }
    }
}
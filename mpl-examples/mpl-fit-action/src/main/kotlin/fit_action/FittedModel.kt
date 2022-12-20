package fit_action

import com.mpl.sdk.MplResponse
import com.mpl.sdk.MplResponseException
import com.mpl.sdk.Payload

class FittedModel(
    private val data: FitProcessData
) {

    fun predict(itemId: String, text: String): MplResponse {
        val value = data.processedData[itemId]?.value
            ?: return MplResponseException(RuntimeException("No element found"))
        return value
            .count { it.contains(text) }
            .let {
                Payload(it.toString())
            }
    }

}
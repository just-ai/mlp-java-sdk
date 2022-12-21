package fit_action

import com.mpl.sdk.MplResponse
import com.mpl.sdk.MplResponseException
import com.mpl.sdk.Payload

class FittedModel(
    private val data: FitProcessData
) {

    fun predict(number: String): MplResponse {
        val value = data.processedData[number]
            ?: return MplResponseException(RuntimeException("No element found"))
        return Payload(value)
    }

}
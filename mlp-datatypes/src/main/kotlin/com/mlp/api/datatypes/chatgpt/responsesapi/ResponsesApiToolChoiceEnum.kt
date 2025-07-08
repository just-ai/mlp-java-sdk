package com.mlp.api.datatypes.chatgpt.responsesapi

import com.fasterxml.jackson.annotation.JsonProperty

/**
* 
* Values: none,auto,required
*/
enum class ResponsesApiToolChoiceEnum(val value: String): ResponsesApiToolChoice {

    @JsonProperty("none") none("none"),
    @JsonProperty("auto") auto("auto"),
    @JsonProperty("required") required("required")
}


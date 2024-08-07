package io.feeba.data

import io.feeba.FeebaFacade
import io.feeba.ServiceLocator
import io.feeba.data.state.AppHistoryState
import io.feeba.lifecycle.LogLevel
import io.feeba.lifecycle.Logger
import kotlinx.serialization.encodeToString
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class RestClient() {

    suspend fun getSurveyPlans(state: AppHistoryState): String? {
        Logger.log(LogLevel.DEBUG, "RestClient::fetchSurveyPlans....")
        return try {
            val response = sendPostRequest(
                "/v2/survey/sdk/list",
                ServiceLocator.jsonInstance.encodeToString(state.userData)
            )
            if (response == "") {
                return null
            }
            Logger.log(LogLevel.DEBUG, "RestClient::plans -> $response")
            return response
        } catch (t: Throwable) {
            Logger.log(LogLevel.ERROR, "RestClient::getSurveyPlans failed: $t")
            null
        }
    }

    private fun sendPostRequest(path: String, jsonInputString: String): String {
        // Create a URL object from the provided URL string
        val requestUrl = "${FeebaFacade.config.serviceConfig.hostUrl}${path}"
        Logger.log(LogLevel.DEBUG, "RestClient::sendPostRequest -> $requestUrl")
        Logger.log(
            LogLevel.DEBUG,
            "RestClient::auth headers -> ${FeebaFacade.config.serviceConfig.apiToken}"
        )
        val url = URL(requestUrl)

        // Open a connection to the URL
        val connection = url.openConnection() as HttpURLConnection

        // Set the request method to POST
        connection.requestMethod = "POST"

        // Set request headers (if needed)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", FeebaFacade.config.serviceConfig.apiToken)

        // Enable input/output streams
        connection.doOutput = true
        connection.doInput = true

        var resultString = ""
        try {
            // Write the JSON payload to the output stream
            val outputStream = DataOutputStream(connection.outputStream)
            outputStream.write(jsonInputString.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()

            // Get the response code
            val responseCode = connection.responseCode

            // Check if the request was successful (HTTP status code 200)
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the input stream
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                resultString = response.toString()
            } else {
                // Handle error cases here (e.g., by throwing an exception or logging)
                throw Exception("POST request failed with response code: $responseCode")
            }
        } finally {
            // Disconnect the connection
            connection.disconnect()
        }
        return resultString
    }
}
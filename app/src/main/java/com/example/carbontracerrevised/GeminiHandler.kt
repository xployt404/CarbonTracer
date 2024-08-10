package com.example.carbontracerrevised


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import com.example.carbontracerrevised.chat.ChatHistory
import com.example.carbontracerrevised.tracer.Traceable
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

class GeminiModel {
    var chatHistoryString = ""
    var generating = false
    companion object{
        private const val TAG = "GeminiModel"
    }


    inner class Text(private val context: Context, private val modelName: String = "gemini-1.5-pro") {
        private lateinit var model: GenerativeModel
        private var apiKey: String = ""

        suspend fun sendPrompt(prompt: String): Deferred<String> = coroutineScope {
            apiKey = ConfigFile.getJsonAttribute(
                ConfigFile.read(context),
                "apiKey").toString()
            model = GenerativeModel(
                modelName,
                apiKey = apiKey,
                systemInstruction = content {
                    text(
                        "You are an assistant that assists in making more eco-friendly choices. " +
                                "You can give explanations to the user about the mentioned subject or matter too if the option presents. " +
                                "If a request by the user is not even indirectly related to environmental sustainability tell the user. " +
                                "You can Answer to short greetings etc."
                    )
                },
                generationConfig = generationConfig {
                    temperature = 0.4f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 4096
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
                )
            )
            generating = true
            chatHistoryString += "input: \n$prompt\n\n"
            async(Dispatchers.IO) {
                model.generateContent(content {
                    text(prompt)

                }).text!!
            }
        }

    }

    inner class Vision {
        suspend fun sendImage(context : Context, images : List<Bitmap>) : String? {
            generating = true
            val model = GenerativeModel(
                "gemini-1.0-pro-vision-latest",
                apiKey = ConfigFile.getJsonAttribute(ConfigFile.read(context), "apiKey").toString()
                // Retrieve API key as an environmental variable defined in a Build Configuration
                // see https://github.com/google/secrets-gradle-plugin for further instructions
                ,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 4096
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
                ),
            )


            if (images.any { false }) {
                throw RuntimeException("Image(s) not found in resources")
            }
            /*
            "You are an assistant that assist in making more eco friendly choices. " +
                            "You are given an image containing one or multiple objects. " +
                            "What is this object? Define how this object or something related to it is connected to Carbon Emissions." +
                            "Give explanations to the user about the mentioned subject or matter too. " +
                            "If you can, quantify how much environmental harm this object produces e.g. in CO2e by giving explanatory calculations and provide preferable official sources and links if possible." +
                            "If you are unable to make out the object tell the user. Answer in ${R.string.language}."
             */

            val content = content {
                text("input: " +
                        "You are an assistant that assist in making more eco friendly choices. " +
                        "Your answer will be processed by another AI that assesses the environmental harm of that object." +
                        "You are given an image containing one or multiple objects. " +
                        "What is the most prominently shown object? " +
                        "Answer with a word group or single word as specific as you can. " +
                        "Answer in lower case without punctuation.")

                image(images[0])
                text("output: ")
            }
            val response = model.generateContent(
                content
            )

            generating = false
            Toast.makeText(context, "finished", Toast.LENGTH_SHORT).show()

            // Alternatively
            print(response.candidates.first().content.parts.first().asTextOrNull())
            return response.text
        }
    }
    inner class File {
        suspend fun sendFile(context : Context, fileUri: Uri, chatHistory: ChatHistory? = null){
            generating = true
            val model = GenerativeModel(
                "gemini-1.5-pro",
                apiKey = ConfigFile.getJsonAttribute(ConfigFile.read(context), "apiKey").toString()
                // Retrieve API key as an environmental variable defined in a Build Configuration
                // see https://github.com/google/secrets-gradle-plugin for further instructions
                ,
                systemInstruction = content{
                    text(
                        "You are an assistant that assists in making more eco-friendly choices. " +
                                "Your purpose is to answer questions regarding ecological sustainability. " +
                                "You are given an audio file that contains a recording which contains the request of the user. " +
                                "Your response contains the spoken words you heard and your answer to the request stated in the audio." +
                                "You can give explanations to the user about the mentioned subject or matter too if the option presents. " +
                                "If a request by the user is not even indirectly related to environmental sustainability tell the user. " +
                                "You can Answer to short greetings etc."+
                                "If you are unable to make out coherent sentences tell the user."
                    )
                },
                generationConfig = generationConfig {
                    temperature = 0.4f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 4096
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
                ),
            )


            val response = model.generateContent(
                content {
                    text("input: ")
                    blob(
                        "audio/ogg", File(fileUri.path!!).readBytes()
                    )
                    text("Listen to the request then respond. ")
                    text("output: " +
                            "spoken_words: \n" +
                            "answer: ")
                }
            )
            Log.d(TAG, response.text.toString())


            // Extracting spoken_words
            val spokenWords = Regex("spoken_words: (.+?)\\n").find(response.text.toString())?.groups?.get(1)?.value?.trim() ?: ""
            chatHistory?.insertChatMessage(false, spokenWords, Instant.now().toEpochMilli().toString())

            // Extracting answer
            val answer =  Regex("answer: (.+)").find(response.text.toString())?.groups?.get(1)?.value?.trim() ?: ""
            chatHistory?.insertChatMessage(true, answer, Instant.now().toEpochMilli().toString())

            generating = false
            // Alternatively
            print(response.candidates.first().content.parts.first().asTextOrNull())
        }
    }
    inner class Tracer{
        suspend fun interpretImage(context : Context, images : List<Bitmap>, chatHistory: ChatHistory? = null) : String {
            generating = true
            val model = GenerativeModel(
                "gemini-1.5-flash",
                apiKey = ConfigFile.getJsonAttribute(ConfigFile.read(context), "apiKey").toString(),
                // Retrieve API key as an environmental variable defined in a Build Configuration
                // see https://github.com/google/secrets-gradle-plugin for further instructions
                generationConfig = generationConfig {
                    temperature = 0.2f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 1000
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
                ),
            )


            if (images.any { false }) {
                throw RuntimeException("Image(s) not found in resources")
            }
            /*
            "You are an assistant that assist in making more eco friendly choices. " +
                            "You are given an image containing one or multiple objects. " +
                            "What is this object? Define how this object or something related to it is connected to Carbon Emissions." +
                            "Give explanations to the user about the mentioned subject or matter too. " +
                            "If you can, quantify how much environmental harm this object produces e.g. in CO2e by giving explanatory calculations and provide preferable official sources and links if possible." +
                            "If you are unable to make out the object tell the user. Answer in ${R.string.language}."
             */

            val content = content {
                text("input: " +
                        "You are given an image containing one or multiple objects. " +
                        "You are tasked to identify and quantify them as accurate as possible to be later processed by a program." +
                        "Fill out the given form." +
                        "Answer in lower case without punctuation." +
                        "What is the most prominently shown object? " +
                        "Name this object with single word or a word group as simple and specific as you can." +
                        "Next try to determine the main material this object is made of." +
                        "Next approximate the amount of the object." +
                        "Prefer to approximate the amount in a unit (metric)." +
                        "If you are unable to approximate it in a metric unit just quantify how much of that object is there and leave the unit blank."
                )

                image(images[0])
                text("output: " )
                text("objectName: \n" +
                        "material: \n" + "amount: \n" +
                        "unit: \n")
            }
            val response = model.generateContent(
                content
            )


            // Get the first text part of the first candidate
            chatHistory?.insertChatMessage(true,
                response.text.toString(), Instant.now().toEpochMilli().toString())
            generating = false


            return response.text!!
        }

        suspend fun generateCo2e(context: Context, traceable: Traceable, fullResponse : Boolean = false): List<String?> {
            generating = true
            val model = GenerativeModel(
                "gemini-1.5-pro",
                apiKey = ConfigFile.getJsonAttribute(ConfigFile.read(context), "apiKey").toString()
                // Retrieve API key as an environmental variable defined in a Build Configuration
                // see https://github.com/google/secrets-gradle-plugin for further instructions
                , systemInstruction = content {
                    text("You are an AI that assists in making more eco-friendly choices." +
                            "Assume the of CO2 emitted by certain objects based on your available data."  +
                            "The objectName is the name of the object to be evaluated." +
                            "The amount represents, in what capacity that object is used. " +
                            "This can be the number of usages of an object but also the amount of the object in liters, grams etc. for instance." +
                            "The occurrence represents the frequency of usage." +
                            "Your task is to calculate the produced CO2e yearly for the occurrence time frame given by the user." +
                            "If the object is a washing machine for instance, the electricity usage is relevant and not the manufacturing and transport emissions." +
                            "For products that are disposed after usage and have to be bought frequently its the other way around of course." +
                            "In the last line of your response only write the amount of CO2 you assumed plus the unit of mass but nothing more."
                    )
                },
                generationConfig = generationConfig {
                    temperature = 0.2f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 800
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
                ),
            )
            println(traceable)
            val content = content {
                text("input: ")
                text("objectName: ${traceable.objectName}\n" +
                        "material: ${traceable.material}\n" +
                        "amount: ${traceable.amount}\n" +
                        "occurrence: ${traceable.occurrence}")


                text("output:" )
            }
            val response = withContext(Dispatchers.IO){
                    model.generateContent(
                        content
                    )
                }


            // Get the first text part of the first candidate
            generating = false
            val splitResponse = response.text!!.lines().filter { it.isNotBlank() }

            println(response.text!!)

            Log.i("CO2E", splitResponse.toString())

            return if (!fullResponse){
                listOf(splitResponse.last())
            } else {
                listOf(splitResponse.last(), response.text)
            }

        }


    }

}
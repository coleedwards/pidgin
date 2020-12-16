package net.evilblock.pidgin

import net.evilblock.pidgin.morph.JsonMorph
import net.evilblock.pidgin.message.Message
import net.evilblock.pidgin.message.handler.IncomingMessageHandler
import net.evilblock.pidgin.message.handler.MessageExceptionHandler
import net.evilblock.pidgin.message.listener.MessageListener
import net.evilblock.pidgin.message.listener.MessageListenerData
import com.google.gson.*
import org.redisson.Redisson
import org.redisson.api.RTopic
import org.redisson.api.RedissonClient
import java.util.ArrayList
import java.util.concurrent.ForkJoinPool
import java.lang.IllegalStateException

/**
 * A Jedis Pub/Sub implementation.
 */
class Pidgin(private val channel: String, private val redisson: RedissonClient, private val options: PidginOptions = PidginOptions()) {

	private var rTopic: RTopic<String> = redisson.getTopic(channel)
	private val messageListeners: MutableList<MessageListenerData> = ArrayList()

	init {
		setupPubSub()
	}

	@JvmOverloads
	fun sendMessage(message: Message, exceptionHandler: MessageExceptionHandler? = null) {
		try {
			val jsonObject = JsonObject()
			jsonObject.addProperty("messageId", message.id)
			jsonObject.add("messageData", morph.fromObject(message.data))

			rTopic.publish(jsonObject.toString())
			if (options.debug) {
				println("[Pidgin] Sent message '${message.id}'")
			}
		} catch (e: Exception) {
			exceptionHandler?.onException(e)
		}
	}

	fun registerListener(messageListener: MessageListener) {
		for (method in messageListener::class.java.declaredMethods) {
			if (method.getDeclaredAnnotation(IncomingMessageHandler::class.java) != null && method.parameters.isNotEmpty()) {
				if (!JsonObject::class.java.isAssignableFrom(method.parameters[0].type)) {
					throw IllegalStateException("First parameter should be of JsonObject type")
				}

				val messageId = method.getDeclaredAnnotation(IncomingMessageHandler::class.java).id

				messageListeners.add(MessageListenerData(messageListener, method, messageId))
			}
		}
	}

	private fun setupPubSub() {
		rTopic.addListener({ channel, msg ->
			try {
				val messagePayload = parser.parse(msg).asJsonObject
				val messageId = messagePayload.get("messageId").asString
				val messageData = messagePayload.get("messageData").asJsonObject

				for (data in messageListeners) {
					if (data.id == messageId) {
						data.method.invoke(data.instance, messageData)
						if (options.debug) {
							println("[Pidgin] Received message '${messageId}'")
						}
					}
				}
			} catch (e: JsonParseException) {
				println("[Pidgin] Expected JSON message but could not parse message")
				e.printStackTrace()
			} catch (e: Exception) {
				println("[Pidgin] Failed to handle message")
				e.printStackTrace()
			}
		})
	}

	companion object {

		val parser = JsonParser()
		val morph = JsonMorph()

	}

}
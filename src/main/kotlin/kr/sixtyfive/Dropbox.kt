package kr.sixtyfive

import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.FileWriter
import java.io.InputStream
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.CompletableFuture

class Dropbox {
	private val gson = GsonBuilder().disableHtmlEscaping().create()

	private val logger = LoggerFactory.getLogger(this::class.java)
	private val client = Http()
	private val baseHeader: Map<String, String>
		get() = mapOf(
			"Authorization" to "Bearer $token",
			"Content-Type" to "application/octet-stream",
		)
	private val token: String
	val user: String?

	constructor(key: String, secret: String, savePath: String? = null) {
		this.token = issueToken(key, secret)
		savePath?.let(::FileWriter)
			?.use { it.write(this.token) }
		this.user = fetchUser(token)
	}

	constructor(token: String) {
		this.token = token
		this.user = fetchUser(this.token)
	}


	private fun fetchUser(token: String): String? {
		return "https://api.dropboxapi.com/2/users/get_current_account"
			.let { client.postAsync(it, headers = mapOf("Authorization" to "Bearer $token")) }
			.get()
			.body()
			.let { URLDecoder.decode(it, UTF_8) }
			.let { gson.fromJson(it, Map::class.java) }.get("name")
			?.let { (it as Map<*, *>).get("display_name") }
			?.let { it as String }
	}

	private fun issueToken(key: String, secret: String): String {
		val requestUrl = "https://www.dropbox.com/oauth2/authorize?client_id=$key&response_type=code"
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			Desktop.getDesktop().browse(URI(requestUrl))
			logger.info("Log in from your browser and copy TOKEN to console")
		} else {
			logger.info("Go to '$requestUrl' and enter the access code")
		}
		val accessCode = readLine()!!.trim()
		val params = mapOf(
			"code" to accessCode,
			"grant_type" to "authorization_code",
			"client_id" to key,
			"client_secret" to secret,
		)
		val token = client.postAsync("https://api.dropboxapi.com/oauth2/token", params = params)
			.get()
			.body()!!
			.let { gson.fromJson(it, Map::class.java) }
			.get("access_token") as String

		logger.info("Issued token: $token")

		return token
	}

	fun authenticate(): Boolean = fetchUser(token) != null

	fun download(fileName: String): CompletableFuture<Pair<InputStream, Response?>> {
		val url = "https://content.dropboxapi.com/2/files/download"
		val params = mapOf("arg" to gson.toJson(mapOf("path" to "/$fileName")))

		return client.postAsync(url, headers = baseHeader, params = params, handler = BodyHandlers.ofInputStream())
			.thenApply {
				val metadata = it.headers().firstValue("Dropbox-API-Result")
					.orElse(null)
					?.let { i -> gson.fromJson(i, Response::class.java) }
				it.body() to metadata
			}
	}

	fun upload(data: InputStream, fileName: String): CompletableFuture<Response?> {
		val url = "https://content.dropboxapi.com/2/files/upload"
		// Since http header cannot handle non-ascii characters correctly, any
		// characters whose codepoint is bigger than 0x7F should have been escaped.
		// But, both of kotlinx-serialization and Gson do not support this kind
		// of http-header-safe-serialization, I chose to use an alternative: `arg` URL parameter.
		val params = mapOf(
			"arg" to gson.toJson(
				mapOf(
					"path" to "/$fileName",
					"mode" to "overwrite",
				)
			)
		)

		return client.postAsync(url, headers = baseHeader, params = params, data = data)
			.thenApply {
				when (it.statusCode()) {
					in 200 until 300 -> URLDecoder.decode(it.body(), UTF_8)
						.let { b -> gson.fromJson(b, Response::class.java) }
					else -> {
						logger.warn("Error occurred while uploading. Status code: ${it.statusCode()}, reason: ${it.body()}")
						null
					}
				}
			}
	}
}


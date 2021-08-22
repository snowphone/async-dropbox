package kr.sixtyfive

import io.github.cdimascio.dotenv.Dotenv
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.test.*

internal class DropboxTest {

	val dotenv = Dotenv
		.load()

	val token = dotenv.get("TOKEN")
	val userName = dotenv.get("USER_NAME")

	@Test
	fun simpleAccessTest() {
		val db = Dropbox(token)
		assertEquals(db.user, userName)
	}

	@Test
	fun intensityTest() {
		val db = Dropbox(token)

		val dataList = (1..12).map {
			val bytes = ByteArray(1_000_000)
			Random().nextBytes(bytes)
			ByteArrayInputStream(bytes)
		}.toList()

		val results = dataList.mapIndexed { it, buf ->
				db.upload(buf, "test/test_$it.txt")
			}
			.map { it.get() != null }
			.toList()

		assertTrue { results.all { it } }
	}

	@Test
	fun createAndReadTest() {
		val db = Dropbox(token)

		val bytes = ByteArray(1_000)
			.also { Random().nextBytes(it) }

		val fileName = "test/CART.txt"
		db.upload(bytes.inputStream(), fileName).get()
			?.let {
				db.download(fileName).get()
			}?.let {
				assertContentEquals(bytes, it.first.readAllBytes())
			} ?: fail("Upload and download should be done")
	}
}
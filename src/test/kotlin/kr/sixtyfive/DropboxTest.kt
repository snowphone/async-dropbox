package kr.sixtyfive

import io.github.cdimascio.dotenv.Dotenv
import java.util.*
import kotlin.test.*

internal class DropboxTest {
	private val dotenv = Dotenv.load()
	private val token = dotenv.get("TOKEN")
	private val userName = dotenv.get("USER_NAME")
	private val db = Dropbox(token)

	@Test
	fun simpleAccessTest() {
		assertEquals(db.user, userName)
	}

	@Test
	fun intensityTest() {
		val dataList = (1..8).map {
			ByteArray(1_000_000)
				.also { Random().nextBytes(it) }
				.inputStream()
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

	@Test
	fun deleteTest() {
		val name = "안녕hello_-_-"
		val bytes = ByteArray(1_000)
			.also { Random().nextBytes(it) }
			.inputStream()
		db.upload(bytes, name)
			.thenCompose {
				it?.run {
					db.delete(name)
				}
			}?.thenApply {
				assertTrue(it)
			} ?: fail("delete test failed")
	}
}
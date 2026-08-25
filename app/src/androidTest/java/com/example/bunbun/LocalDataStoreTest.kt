package com.example.bunbun

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bunbun.data.local.BunbunDatabase
import com.example.bunbun.data.local.LocalDataStore
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.model.MessageDto
import com.example.bunbun.data.model.MessagesData
import com.example.bunbun.data.model.UserDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class LocalDataStoreTest {
    private lateinit var context: Context
    private lateinit var database: BunbunDatabase
    private var clock = 1_000L
    private var clientSequence = 0

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DB_NAME)
        database = openDatabase()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(DB_NAME)
    }

    @Test fun pendingMessagesArePersistentOrderedAndMergedByClientId() = runBlocking {
        val store = store()
        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer A")))

        val a = store.queueOutgoing(ACCOUNT_A, CHAT_ID, ACCOUNT_A, "A")
        clock++
        val b = store.queueOutgoing(ACCOUNT_A, CHAT_ID, ACCOUNT_A, "B")
        clock++
        val c = store.queueOutgoing(ACCOUNT_A, CHAT_ID, ACCOUNT_A, "C")

        assertEquals(listOf("A", "B", "C"), store.pendingMessages(ACCOUNT_A).map { it.text })
        database.close()
        database = openDatabase()
        val restored = LocalDataStore(database)
        assertEquals(listOf("A", "B", "C"), restored.pendingMessages(ACCOUNT_A).map { it.text })

        restored.acknowledge(
            ACCOUNT_A,
            ACCOUNT_A,
            MessageDto(101, CHAT_ID, ACCOUNT_A, "A", "2026-08-14T10:00:00Z", a.clientMessageId),
        )
        val messages = restored.observeMessages(ACCOUNT_A, CHAT_ID).first()
        assertEquals(3, messages.size)
        assertEquals(101L, messages.first { it.clientMessageId == a.clientMessageId }.serverId)
        assertEquals(MessageSendState.SENT, messages.first { it.clientMessageId == a.clientMessageId }.sendState)
        assertNotEquals(b.clientMessageId, c.clientMessageId)

        restored.mergeMessages(ACCOUNT_A, ACCOUNT_A, CHAT_ID, MessagesData(emptyList(), 101))
        assertEquals(
            MessageSendState.READ,
            restored.observeMessages(ACCOUNT_A, CHAT_ID).first().first { it.clientMessageId == a.clientMessageId }.sendState,
        )
    }

    @Test fun identicalServerAndChatIdsRemainAccountIsolated() = runBlocking {
        val store = store()
        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer A")))
        store.mergeChats(ACCOUNT_B, listOf(chat(peerName = "Peer B")))
        store.mergeMessages(
            ACCOUNT_A,
            ACCOUNT_A,
            CHAT_ID,
            MessagesData(listOf(MessageDto(5, CHAT_ID, ACCOUNT_A, "A only", "2026-08-14T10:00:00Z"))),
        )
        store.mergeMessages(
            ACCOUNT_B,
            ACCOUNT_B,
            CHAT_ID,
            MessagesData(listOf(MessageDto(5, CHAT_ID, ACCOUNT_B, "B only", "2026-08-14T10:00:00Z"))),
        )

        assertEquals("Peer A", store.observeChats(ACCOUNT_A).first().single().peerDisplayName)
        assertEquals("Peer B", store.observeChats(ACCOUNT_B).first().single().peerDisplayName)
        assertEquals("A only", store.observeMessages(ACCOUNT_A, CHAT_ID).first().single().text)
        assertEquals("B only", store.observeMessages(ACCOUNT_B, CHAT_ID).first().single().text)
    }

    @Test fun peerLastSeenSurvivesOfflineRestartAndNetworkMergeUpdatesIt() = runBlocking {
        val firstSeen = "2026-08-23T19:17:00Z"
        val refreshedSeen = "2026-08-24T07:42:00Z"
        val store = store()
        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer", lastSeenAt = firstSeen)))
        val firstMillis = Instant.parse(firstSeen).toEpochMilli()
        assertEquals(firstMillis, store.observeChat(ACCOUNT_A, CHAT_ID).first()?.peerLastSeenAtMillis)

        database.close()
        database = openDatabase()
        val offlineStore = LocalDataStore(database)
        assertEquals(firstMillis, offlineStore.observeChat(ACCOUNT_A, CHAT_ID).first()?.peerLastSeenAtMillis)

        offlineStore.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer", lastSeenAt = refreshedSeen)))
        assertEquals(
            Instant.parse(refreshedSeen).toEpochMilli(),
            offlineStore.observeChat(ACCOUNT_A, CHAT_ID).first()?.peerLastSeenAtMillis,
        )
    }

    @Test fun currentUsersReadCursorIsCachedAndNeverMovesBackward() = runBlocking {
        val store = store()
        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer", myReadCursor = 42)))
        assertEquals(42L, store.observeChat(ACCOUNT_A, CHAT_ID).first()?.myLastReadMessageId)

        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer", myReadCursor = 21)))
        assertEquals(42L, store.observeChat(ACCOUNT_A, CHAT_ID).first()?.myLastReadMessageId)
    }

    @Test fun profileUpdateIsCachedWithoutChangingUsername() = runBlocking {
        val store = store()
        val original = UserDto(ACCOUNT_A, "stable_login", "Иван", "2026-08-24T00:00:00Z")
        store.cacheCurrentUser(original)
        store.cacheCurrentUser(original.copy(displayName = "Иван П."))

        val cached = store.cachedCurrentUser(ACCOUNT_A)!!
        assertEquals("stable_login", cached.username)
        assertEquals("Иван П.", cached.displayName)
    }

    @Test fun clearAccountDataRemovesChatsMessagesProfileAndPendingWithoutTouchingOtherAccount() = runBlocking {
        val store = store()
        store.cacheCurrentUser(UserDto(ACCOUNT_A, "account_a", "A", "2026-08-24T00:00:00Z"))
        store.cacheCurrentUser(UserDto(ACCOUNT_B, "account_b", "B", "2026-08-24T00:00:00Z"))
        store.mergeChats(ACCOUNT_A, listOf(chat(peerName = "Peer A")))
        store.mergeChats(ACCOUNT_B, listOf(chat(peerName = "Peer B")))
        store.queueOutgoing(ACCOUNT_A, CHAT_ID, ACCOUNT_A, "pending A")
        store.queueOutgoing(ACCOUNT_B, CHAT_ID, ACCOUNT_B, "pending B")

        store.clearAccountData(ACCOUNT_A)

        assertTrue(store.observeChats(ACCOUNT_A).first().isEmpty())
        assertTrue(store.observeMessages(ACCOUNT_A, CHAT_ID).first().isEmpty())
        assertEquals(0, store.pendingMessageCount(ACCOUNT_A))
        assertEquals(null, store.cachedCurrentUser(ACCOUNT_A))
        assertEquals("Peer B", store.observeChats(ACCOUNT_B).first().single().peerDisplayName)
        assertEquals("pending B", store.observeMessages(ACCOUNT_B, CHAT_ID).first().single().text)
        assertEquals("account_b", store.cachedCurrentUser(ACCOUNT_B)?.username)
    }

    private fun store() = LocalDataStore(database, now = { clock }, newClientId = { "00000000-0000-4000-8000-${(++clientSequence).toString().padStart(12, '0')}" })

    private fun openDatabase() = Room.databaseBuilder(context, BunbunDatabase::class.java, DB_NAME)
        .allowMainThreadQueries()
        .build()

    private fun chat(peerName: String, lastSeenAt: String? = null, myReadCursor: Long? = null) = ChatDto(
        id = CHAT_ID,
        type = "direct",
        peer = UserDto(99, "peer", peerName, "2026-08-14T09:00:00Z", lastSeenAt),
        myLastReadMessageId = myReadCursor,
    )

    private companion object {
        const val DB_NAME = "bunbun-room-test.db"
        const val CHAT_ID = 10L
        const val ACCOUNT_A = 1L
        const val ACCOUNT_B = 2L
    }
}

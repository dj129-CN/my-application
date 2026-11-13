package com.example.noteflow.provider

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.pm.PermissionInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the todo provider through a probe bound in the :todoProbe process.
 * The probe pid is asserted to differ from the test process pid, so the CRUD
 * operations genuinely cross a Binder/process boundary. The same test also
 * verifies notification delivery and the signature permission declaration.
 */
@RunWith(AndroidJUnit4::class)
class TodoContentProviderTest {

    private lateinit var context: Context
    private lateinit var resolver: ContentResolver
    private lateinit var connection: ServiceConnection
    private var bound = false
    private val connected = CountDownLatch(1)
    private val messengerRef = AtomicReference<Messenger>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resolver = context.contentResolver
        resolver.delete(TodoContract.CONTENT_URI, null, null)

        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service != null) {
                    messengerRef.set(Messenger(service))
                    connected.countDown()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                messengerRef.set(null)
            }
        }

        var requested = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            requested = context.bindService(
                Intent(context, TodoProviderProbeService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
        }
        assertTrue("bindService returned false", requested)
        assertTrue("probe service did not bind", connected.await(15, TimeUnit.SECONDS))
        assertNotNull(messengerRef.get())
        bound = true
    }

    @After
    fun tearDown() {
        if (bound) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                context.unbindService(connection)
            }
        }
        resolver.delete(TodoContract.CONTENT_URI, null, null)
    }

    @Suppress("DEPRECATION")
    @Test
    fun providerRequiresSignaturePermission() {
        val permissionInfo = context.packageManager.getPermissionInfo(TodoContract.PERMISSION, 0)
        assertEquals(
            PermissionInfo.PROTECTION_SIGNATURE,
            permissionInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE,
        )

        val providerInfo = context.packageManager.resolveContentProvider(TodoContract.AUTHORITY, 0)
        assertNotNull("Provider not found: ${TodoContract.AUTHORITY}", providerInfo)
        requireNotNull(providerInfo)
        assertTrue("Provider must be exported for cross-app callers", providerInfo.exported)
        assertEquals(TodoContract.PERMISSION, providerInfo.readPermission)
        assertEquals(TodoContract.PERMISSION, providerInfo.writePermission)
    }

    @Test
    fun crossProcessCrudAndNotifyChange() {
        val notification = CountDownLatch(1)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                notification.countDown()
            }
        }
        resolver.registerContentObserver(TodoContract.CONTENT_URI, true, observer)

        try {
            val inserted = call(
                TodoProviderProbeService.OP_INSERT,
            ) {
                putString(TodoProviderProbeService.KEY_TITLE, "跨进程待办")
                putString(TodoProviderProbeService.KEY_CONTENT, "provider test")
                putString(TodoProviderProbeService.KEY_PRIORITY, TodoContract.PRIORITY_HIGH)
                putInt(TodoProviderProbeService.KEY_COMPLETED, 0)
            }
            val id = inserted.getLong(TodoProviderProbeService.KEY_ID)
            assertTrue("insert must return a positive id", id > 0L)
            assertNotEquals(Process.myPid(), inserted.getInt(TodoProviderProbeService.KEY_PID, -1))
            assertTrue("notifyChange was not delivered", notification.await(10, TimeUnit.SECONDS))

            val queried = call(
                TodoProviderProbeService.OP_QUERY,
            ) {
                putString(
                    TodoProviderProbeService.KEY_URI,
                    TodoContract.contentUriFor(id).toString(),
                )
            }
            assertEquals(1, queried.getInt(TodoProviderProbeService.KEY_COUNT))
            assertEquals("跨进程待办", queried.getString(TodoProviderProbeService.KEY_TITLE))
            assertEquals(
                TodoContract.PRIORITY_HIGH,
                queried.getString(TodoProviderProbeService.KEY_PRIORITY),
            )

            val updated = call(
                TodoProviderProbeService.OP_UPDATE,
            ) {
                putLong(TodoProviderProbeService.KEY_ID, id)
                putString(TodoProviderProbeService.KEY_TITLE, "跨进程已更新")
                putInt(TodoProviderProbeService.KEY_COMPLETED, 1)
            }
            assertEquals(1, updated.getInt(TodoProviderProbeService.KEY_ROWS))

            resolver.query(
                TodoContract.contentUriFor(id),
                arrayOf(
                    TodoContract.COLUMN_TITLE,
                    TodoContract.COLUMN_IS_COMPLETED,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("跨进程已更新", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }

            val filtered = call(
                TodoProviderProbeService.OP_QUERY,
            ) {
                putString(
                    TodoProviderProbeService.KEY_URI,
                    TodoContract.contentUriForCompleted(true).toString(),
                )
            }
            assertEquals(1, filtered.getInt(TodoProviderProbeService.KEY_COUNT))

            val deleted = call(
                TodoProviderProbeService.OP_DELETE,
            ) {
                putLong(TodoProviderProbeService.KEY_ID, id)
            }
            assertEquals(1, deleted.getInt(TodoProviderProbeService.KEY_ROWS))

            resolver.query(TodoContract.CONTENT_URI, null, null, null, null)?.use { cursor ->
                assertEquals(0, cursor.count)
            }
        } finally {
            resolver.unregisterContentObserver(observer)
        }
    }

    private fun call(
        operation: String,
        configure: Bundle.() -> Unit = {},
    ): Bundle {
        val responseRef = AtomicReference<Bundle>()
        val responseLatch = CountDownLatch(1)
        val replyHandler = Handler(Looper.getMainLooper()) { message ->
            responseRef.set(message.data)
            responseLatch.countDown()
            true
        }

        val data = Bundle().apply {
            putString(TodoProviderProbeService.KEY_OPERATION, operation)
            configure(this)
        }
        val request = Message.obtain(null, TodoProviderProbeService.MSG_REQUEST).apply {
            this.data = data
            replyTo = Messenger(replyHandler)
        }

        val messenger = requireNotNull(messengerRef.get()) { "Probe service is not connected" }
        messenger.send(request)
        assertTrue(
            "probe service timeout for $operation",
            responseLatch.await(15, TimeUnit.SECONDS),
        )

        val response = requireNotNull(responseRef.get())
        if (!response.getBoolean(TodoProviderProbeService.KEY_SUCCESS)) {
            throw AssertionError(
                "probe operation '$operation' failed: " +
                    response.getString(TodoProviderProbeService.KEY_ERROR),
            )
        }
        return response
    }
}

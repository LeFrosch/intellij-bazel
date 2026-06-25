package com.google.idea.blaze.base.command.buildresult.bepparser

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.protobuf.CodedInputStream
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.LimitedInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.milliseconds

private val LOG = Logger.getInstance("BepStreamer")

private suspend fun readEvent(stream: InputStream): BuildEvent? {
  // make sure that there are at least four bytes already available
  while (stream.available() < 4) {
    delay(10.milliseconds)
  }

  // protobuf messages are delimited by size (encoded as varint32),
  // read size manually to ensure the entire message is already available
  val size = CodedInputStream.readRawVarint32(stream.read(), stream)

  while (stream.available() < size) {
    delay(10.milliseconds)
  }

  val eventStream = LimitedInputStream(stream, size)

  return try {
    BuildEvent.parseFrom(eventStream)
  } catch (e: Exception) {
    LOG.error("could not parse event", e)

    // if the message could not be parsed, make sure to skip it
    if (eventStream.bytesRead < size) {
      stream.skip(size.toLong() - eventStream.bytesRead)
    }

    null
  }
}

fun streamBepEvents(path: Path): Flow<BuildEvent> {
  return flow {
    val stream = withContext(Dispatchers.IO) {
      Files.newInputStream(path).buffered()
    }

    stream.use { stream ->
      var size = Files.size(path)

      coroutineScope {
        // keep reading events while the coroutine is active or while the stream
        // has data available (to ensure that all events are processed)
        while (isActive || stream.available() > 0) {
          // detect if the file has been truncated
          val currentSize = Files.size(path)
          if (currentSize < size) break
          size = currentSize

          val event = withContext(Dispatchers.IO) { readEvent(stream) } ?: continue
          emit(event)

          // detect if the build has
          if (event.id.hasBuildFinished()) break
        }
      }
    }
  }
}


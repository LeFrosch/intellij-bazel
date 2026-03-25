package com.google.idea.base.src.com.google.idea.blaze.base.buildview

import com.google.idea.blaze.base.buildview.BuildViewScope
import com.google.idea.blaze.base.buildview.pushJob
import com.google.idea.blaze.base.scope.BlazeContext
import com.google.idea.blaze.base.scope.output.IssueOutput
import com.google.idea.blaze.base.scope.scopes.TimingScope
import com.google.idea.blaze.base.util.pluginProjectScope
import com.intellij.build.events.MessageEvent
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.Future

/** Represents a routine that can be run on top of a BlazeContext. */
interface ContextRoutine<T> {

  val name: String

  val propagateErrors: Boolean get() = true

  @Throws(ExecutionException::class)
  fun run(ctx: BlazeContext): T
}

fun <T> ContextRoutine<T>.launch(ctx: BlazeContext): T? {
  return BlazeContext.create(ctx).use { childCtx ->
    childCtx.push(TimingScope(name, TimingScope.EventType.BlazeInvocation))
    childCtx.setPropagatesErrors(propagateErrors)

    try {
      run(childCtx)
    } catch (e: ExecutionException) {
      IssueOutput.Builder(
        kind = if (propagateErrors) MessageEvent.Kind.ERROR else MessageEvent.Kind.WARNING,
        title = "$name failed",
      ).withThrowable(e).submit(ctx)

      return null
    }
  }
}

/** Represents a root build routine executed on top of the root BlazeContext. */
fun interface BuildRoutine<T> {

  @Throws(ExecutionException::class)
  fun run(ctx: BlazeContext): T?
}

@Throws(ExecutionException::class)
fun <T> buildRoutine(project: Project, title: String, body: BuildRoutine<T>): Future<T> {
  return pluginProjectScope(project).async(Dispatchers.Default) {
    BlazeContext.create().use { ctx ->
      ctx.push(TimingScope(title, TimingScope.EventType.Other))
      ctx.push(BuildViewScope.forBuild(project, title))

      try {
        ctx.pushJob { body.run(ctx) }
      } catch (e: ExecutionException) {
        IssueOutput.error(e.message ?: "Unknown error").withThrowable(e).submit(ctx)
        throw e
      }
    }
  }.asCompletableFuture()
}
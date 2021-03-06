package com.softwaremill.sttp.asynchttpclient.cats

import java.nio.ByteBuffer

import cats.effect.Async
import com.softwaremill.sttp.asynchttpclient.AsyncHttpClientBackend
import com.softwaremill.sttp.impl.cats.AsyncMonadAsyncError
import com.softwaremill.sttp.{FollowRedirectsBackend, SttpBackend, SttpBackendOptions}
import io.netty.buffer.ByteBuf
import org.asynchttpclient.{
  AsyncHttpClient,
  AsyncHttpClientConfig,
  DefaultAsyncHttpClient,
  DefaultAsyncHttpClientConfig
}
import org.reactivestreams.Publisher

import scala.language.higherKinds

class AsyncHttpClientCatsBackend[F[_]: Async] private (
    asyncHttpClient: AsyncHttpClient,
    closeClient: Boolean
) extends AsyncHttpClientBackend[F, Nothing](
      asyncHttpClient,
      new AsyncMonadAsyncError,
      closeClient
    ) {
  override protected def streamBodyToPublisher(s: Nothing): Publisher[ByteBuf] =
    s // nothing is everything

  override protected def publisherToStreamBody(p: Publisher[ByteBuffer]): Nothing =
    throw new IllegalStateException("This backend does not support streaming")

  override protected def publisherToBytes(p: Publisher[ByteBuffer]): F[Array[Byte]] =
    throw new IllegalStateException("This backend does not support streaming")
}

object AsyncHttpClientCatsBackend {

  private def apply[F[_]: Async](asyncHttpClient: AsyncHttpClient, closeClient: Boolean): SttpBackend[F, Nothing] =
    new FollowRedirectsBackend[F, Nothing](new AsyncHttpClientCatsBackend(asyncHttpClient, closeClient))

  def apply[F[_]: Async](options: SttpBackendOptions = SttpBackendOptions.Default): SttpBackend[F, Nothing] =
    AsyncHttpClientCatsBackend(AsyncHttpClientBackend.defaultClient(options), closeClient = true)

  def usingConfig[F[_]: Async](cfg: AsyncHttpClientConfig): SttpBackend[F, Nothing] =
    AsyncHttpClientCatsBackend(new DefaultAsyncHttpClient(cfg), closeClient = true)

  /**
    * @param updateConfig A function which updates the default configuration (created basing on `options`).
    */
  def usingConfigBuilder[F[_]: Async](
      updateConfig: DefaultAsyncHttpClientConfig.Builder => DefaultAsyncHttpClientConfig.Builder,
      options: SttpBackendOptions = SttpBackendOptions.Default
  ): SttpBackend[F, Nothing] =
    AsyncHttpClientCatsBackend(
      AsyncHttpClientBackend.clientWithModifiedOptions(options, updateConfig),
      closeClient = true
    )

  def usingClient[F[_]: Async](client: AsyncHttpClient): SttpBackend[F, Nothing] =
    AsyncHttpClientCatsBackend(client, closeClient = false)
}

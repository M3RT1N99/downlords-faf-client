package com.faforever.client.chat;

import com.faforever.client.chat.UrlPreviewResolver.PreviewData;
import com.faforever.client.test.ServiceTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class UrlPreviewResolverImplTest extends ServiceTest {

  @InjectMocks
  private UrlPreviewResolverImpl instance;

  @Mock
  private ObjectFactory<WebClient> userWebClientFactory;
  @Spy
  private WebClient webClient;

  private MockWebServer mockWebServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    webClient = WebClient.builder().build();
    when(userWebClientFactory.getObject()).thenReturn(webClient);
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testResolvePreviewHtml() throws Exception {
    String html = "<html><head><meta property=\"og:title\" content=\"Test Title\" /><meta property=\"og:description\" content=\"Test Description\" /><meta property=\"og:image\" content=\"http://example.com/image.png\" /></head><body></body></html>";
    mockWebServer.enqueue(new MockResponse()
        .setBody(html)
        .addHeader("Content-Type", "text/html"));

    String url = mockWebServer.url("/test").toString();
    Optional<PreviewData> preview = instance.resolvePreview(url).join();

    assertTrue(preview.isPresent());
    assertEquals("Test Title", preview.get().title());
    assertEquals("Test Description", preview.get().description());
    assertEquals("http://example.com/image.png", preview.get().imageUrl());
  }

  @Test
  public void testResolvePreviewImage() throws Exception {
    mockWebServer.enqueue(new MockResponse()
        .setBody("image data")
        .addHeader("Content-Type", "image/png"));

    String url = mockWebServer.url("/image.png").toString();
    Optional<PreviewData> preview = instance.resolvePreview(url).join();

    assertTrue(preview.isPresent());
    assertEquals(url, preview.get().imageUrl());
    assertEquals(null, preview.get().title());
    assertEquals(null, preview.get().description());
  }
}

package com.faforever.client.chat;

import com.faforever.client.config.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Lazy
@Component
@RequiredArgsConstructor
@Slf4j
public class UrlPreviewResolverImpl implements UrlPreviewResolver {

  private static final Pattern OG_TITLE = Pattern.compile("<meta property=\"og:title\" content=\"(.*?)\"");
  private static final Pattern OG_DESCRIPTION = Pattern.compile("<meta property=\"og:description\" content=\"(.*?)\"");
  private static final Pattern OG_IMAGE = Pattern.compile("<meta property=\"og:image\" content=\"(.*?)\"");
  private static final Pattern TITLE_TAG = Pattern.compile("<title>(.*?)</title>");

  @Qualifier("userWebClient")
  private final ObjectFactory<WebClient> userWebClientFactory;

  @Override
  @Cacheable(value = CacheNames.URL_PREVIEW, sync = true)
  @Async
  public CompletableFuture<Optional<PreviewData>> resolvePreview(String urlString) {
    try {
      URI uri = URI.create(urlString);
      if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
        return CompletableFuture.completedFuture(Optional.empty());
      }

      return userWebClientFactory.getObject().get()
          .uri(uri)
          .exchangeToMono(response -> {
            MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
            if (contentType.isCompatibleWith(MediaType.IMAGE_JPEG) || contentType.isCompatibleWith(MediaType.IMAGE_PNG)) {
              return response.releaseBody().thenReturn(Optional.of(new PreviewData(urlString, null, null)));
            } else if (contentType.isCompatibleWith(MediaType.TEXT_HTML)) {
              return response.bodyToMono(String.class).map(html -> parseHtml(html, urlString));
            } else {
              return response.releaseBody().thenReturn(Optional.<PreviewData>empty());
            }
          })
          .toFuture();

    } catch (Exception e) {
      log.debug("Could not resolve preview for url {}", urlString, e);
      return CompletableFuture.completedFuture(Optional.empty());
    }
  }

  private Optional<PreviewData> parseHtml(String html, String url) {
    String title = extract(html, OG_TITLE);
    if (title == null) {
      title = extract(html, TITLE_TAG);
    }
    String description = extract(html, OG_DESCRIPTION);
    String image = extract(html, OG_IMAGE);

    if (title == null && image == null) {
      return Optional.empty();
    }

    return Optional.of(new PreviewData(image, title, description));
  }

  private String extract(String html, Pattern pattern) {
    Matcher matcher = pattern.matcher(html);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}

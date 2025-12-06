package com.faforever.client.chat;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UrlPreviewResolver {

  CompletableFuture<Optional<PreviewData>> resolvePreview(String urlString);

  record PreviewData(String imageUrl, String title, String description) {}
}

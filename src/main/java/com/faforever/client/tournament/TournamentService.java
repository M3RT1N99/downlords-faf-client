package com.faforever.client.tournament;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.domain.api.Tournament;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mapstruct.TournamentMapper;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;

@Lazy
@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentService {
  private final FafApiAccessor fafApiAccessor;
  private final TournamentMapper tournamentMapper;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final PlatformService platformService;

  public Flux<Tournament> getAllTournaments() {
    return fafApiAccessor.getMany(com.faforever.commons.api.dto.Tournament.class, "/challonge/v1/tournaments.json", 100,
                                  Map.of()).map(tournamentMapper::map).cache();
  }

  public void checkForUpcomingTournaments() {
    getAllTournaments().filter(tournament -> tournament.startingAt() != null)
                       .filter(tournament -> tournament.startingAt().isAfter(OffsetDateTime.now()))
                       .filter(tournament -> tournament.startingAt().isBefore(OffsetDateTime.now().plusHours(24)))
                       .collectList()
                       .subscribe(tournaments -> {
                         if (tournaments.isEmpty()) {
                           return;
                         }

                         int count = tournaments.size();
                         String title = i18n.get("tournament.upcoming.title");
                         String message = i18n.get("tournament.upcoming.message", count);

                         Action action = new Action(i18n.get("tournament.view"),
                             () -> platformService.showDocument("https://faforever.com/tournaments"));

                         notificationService.addNotification(
                             new PersistentNotification(message, Severity.INFO, Collections.singletonList(action)));
                       });
  }
}
